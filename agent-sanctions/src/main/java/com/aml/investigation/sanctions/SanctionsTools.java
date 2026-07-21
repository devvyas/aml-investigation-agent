package com.aml.investigation.sanctions;

import com.aml.investigation.sanctions.model.AdverseMediaResult;
import com.aml.investigation.sanctions.model.PepResult;
import com.aml.investigation.sanctions.model.SanctionsMatch;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The @Tool-facing layer, thin and delegating — same reasoning as KycTools in agent-kyc.
 */
@Component
public class SanctionsTools {

    private final SanctionsListClient sanctionsListClient;

    public SanctionsTools(SanctionsListClient sanctionsListClient) {
        this.sanctionsListClient = sanctionsListClient;
    }

    @Tool("Screens a batch of entity names against sanctions watchlists (e.g. OFAC SDN) and " +
            "returns fuzzy-match results with a score, the list matched against, and the " +
            "match type.")
    public List<SanctionsMatch> batchSanctionsCheck(
            @P("the entity names to screen, e.g. account holders, directors, registered agents") List<String> entityNames) {
        return sanctionsListClient.batchSanctionsCheck(entityNames);
    }

    @Tool("Checks whether a named person is a politically exposed person (PEP).")
    public PepResult getPepStatus(@P("the person identifier") String personId) {
        return sanctionsListClient.getPepStatus(personId);
    }

    @Tool("Searches for adverse media coverage of a given entity.")
    public AdverseMediaResult checkAdverseMedia(@P("the entity identifier") String entityId) {
        return sanctionsListClient.checkAdverseMedia(entityId);
    }
}
