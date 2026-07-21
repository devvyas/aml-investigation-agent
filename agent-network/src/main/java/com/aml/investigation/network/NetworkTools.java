package com.aml.investigation.network;

import com.aml.investigation.network.model.LinkedAccount;
import com.aml.investigation.network.model.StructuringResult;
import com.aml.investigation.network.model.TransactionNetwork;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The @Tool-facing layer, thin and delegating — same reasoning as KycTools/SanctionsTools.
 * TransactionGraphRepository owns every line of Cypher; this class only shapes what the model
 * sees.
 */
@Component
public class NetworkTools {

    private final TransactionGraphRepository graphRepository;

    public NetworkTools(TransactionGraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    @Tool("Traverses the transaction graph outward from an account up to the given depth, " +
            "returning the number of reachable accounts, circular transaction flows detected, " +
            "and structuring patterns found.")
    public TransactionNetwork graphTraverse(
            @P("the account identifier") String accountId,
            @P("how many hops to traverse outward, typically 2-4") int depth) {
        return graphRepository.graphTraverse(accountId, depth);
    }

    @Tool("Lists accounts directly linked to the given account by a transfer, with the total " +
            "amount transferred to each.")
    public List<LinkedAccount> getLinkedAccounts(@P("the account identifier") String accountId) {
        return graphRepository.getLinkedAccounts(accountId);
    }

    @Tool("Detects structuring — transfers clustered just under the reporting threshold — " +
            "within a recent time window for the given account.")
    public StructuringResult detectStructuring(
            @P("the account identifier") String accountId,
            @P("how many days back to look, e.g. 30") int windowDays) {
        return graphRepository.detectStructuring(accountId, Duration.ofDays(windowDays));
    }
}
