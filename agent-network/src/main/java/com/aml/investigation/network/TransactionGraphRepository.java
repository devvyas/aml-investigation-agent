package com.aml.investigation.network;

import com.aml.investigation.network.model.LinkedAccount;
import com.aml.investigation.network.model.StructuringResult;
import com.aml.investigation.network.model.TransactionNetwork;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

/**
 * Raw Neo4j driver, hand-written Cypher, manual result mapping — Phase 3's deliberate choice
 * over Spring Data Neo4j, for full explicit control on a path that feeds a SAR filing decision.
 * No LangChain4j, no model awareness anywhere in this class; see NetworkTools for that seam.
 *
 * <p>Structuring is defined here as transfers landing just under the $10,000 CTR reporting
 * threshold — the classic evasion pattern from the domain brief.
 */
@Component
public class TransactionGraphRepository {

    private static final double STRUCTURING_FLOOR = 9000.0;
    private static final double STRUCTURING_CEILING = 10000.0;
    private static final int STRUCTURING_OCCURRENCE_THRESHOLD = 3;
    private static final int MAX_TRAVERSAL_DEPTH = 10;

    private final Driver driver;

    public TransactionGraphRepository(Driver driver) {
        this.driver = driver;
    }

    public TransactionNetwork graphTraverse(String accountId, int depth) {
        int boundedDepth = Math.min(Math.max(depth, 1), MAX_TRAVERSAL_DEPTH);

        // Cypher does not allow parameterizing a variable-length relationship bound
        // (*1..$depth is not valid) — depth is validated/clamped above, then interpolated
        // directly, while accountId stays a proper bound parameter.
        String query = """
                MATCH (origin:Account {accountId: $accountId})-[:TRANSFERRED_TO*1..%d]->(reached:Account)
                WITH origin, collect(DISTINCT reached) AS reachable
                OPTIONAL MATCH cyclePath = (origin)-[:TRANSFERRED_TO*2..%d]->(origin)
                WITH origin, reachable, count(DISTINCT cyclePath) AS circularFlows
                OPTIONAL MATCH (a:Account)-[t:TRANSFERRED_TO]->(:Account)
                WHERE a IN reachable + origin AND t.amount >= $floor AND t.amount < $ceiling
                RETURN size(reachable) AS nodeCount, circularFlows, count(t) AS structuringPatterns
                """.formatted(boundedDepth, boundedDepth);

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Record record = tx.run(query, Map.of(
                        "accountId", accountId,
                        "floor", STRUCTURING_FLOOR,
                        "ceiling", STRUCTURING_CEILING
                )).single();

                return new TransactionNetwork(
                        record.get("nodeCount").asInt(),
                        record.get("circularFlows").asInt(),
                        record.get("structuringPatterns").asInt()
                );
            });
        }
    }

    public List<LinkedAccount> getLinkedAccounts(String accountId) {
        String query = """
                MATCH (:Account {accountId: $accountId})-[t:TRANSFERRED_TO]->(other:Account)
                RETURN other.accountId AS accountId, sum(t.amount) AS totalTransferAmount
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run(query, Map.of("accountId", accountId))
                    .list(record -> new LinkedAccount(
                            record.get("accountId").asString(),
                            "TRANSFERRED_TO",
                            record.get("totalTransferAmount").asDouble()
                    )));
        }
    }

    public StructuringResult detectStructuring(String accountId, java.time.Duration window) {
        Instant windowStart = Instant.now().minus(window);

        String query = """
                MATCH (:Account {accountId: $accountId})-[t:TRANSFERRED_TO]->(:Account)
                WHERE t.timestamp >= $windowStart AND t.amount >= $floor AND t.amount < $ceiling
                RETURN count(t) AS occurrenceCount, min(t.amount) AS minAmount, max(t.amount) AS maxAmount
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Record record = tx.run(query, Map.of(
                        "accountId", accountId,
                        "windowStart", windowStart.toEpochMilli(),
                        "floor", STRUCTURING_FLOOR,
                        "ceiling", STRUCTURING_CEILING
                )).single();

                int occurrenceCount = record.get("occurrenceCount").asInt();
                boolean patternFound = occurrenceCount >= STRUCTURING_OCCURRENCE_THRESHOLD;

                return new StructuringResult(
                        patternFound,
                        record.get("minAmount").isNull() ? 0.0 : record.get("minAmount").asDouble(),
                        record.get("maxAmount").isNull() ? 0.0 : record.get("maxAmount").asDouble(),
                        occurrenceCount
                );
            });
        }
    }
}
