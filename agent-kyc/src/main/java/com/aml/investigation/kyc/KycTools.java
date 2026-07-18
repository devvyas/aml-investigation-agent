package com.aml.investigation.kyc;

import com.aml.investigation.kyc.model.CorporateStructureResult;
import com.aml.investigation.kyc.model.DocumentVerificationResult;
import com.aml.investigation.kyc.model.RegisteredAgent;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The @Tool-facing layer the model actually calls. Stays thin and delegates immediately —
 * no HTTP, no retries, no parsing here. See Phase 3's reasoning for why that lives in
 * CorporateRegistryClient instead: a tool method's signature is reflected into the model's
 * JSON schema, which makes it a prompt-engineering surface, not an integration layer.
 */
@Component
public class KycTools {

    private final CorporateRegistryClient registryClient;

    public KycTools(CorporateRegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Tool("Resolves the corporate structure for an account: how many layers of entities sit " +
            "between the account and its ultimate parent, whether a beneficial owner is " +
            "disclosed, and which jurisdictions are involved.")
    public CorporateStructureResult resolveCorporateStructure(
            @P("the account identifier") String accountId) {
        return registryClient.resolveCorporateStructure(accountId);
    }

    @Tool("Lists the registered agents on file for a given corporate entity.")
    public List<RegisteredAgent> getRegisteredAgents(
            @P("the entity identifier, typically obtained from resolveCorporateStructure") String entityId) {
        return registryClient.getRegisteredAgents(entityId);
    }

    @Tool("Verifies whether the documents submitted for an account pass registry checks.")
    public DocumentVerificationResult verifyDocuments(
            @P("the account identifier") String accountId) {
        return registryClient.verifyDocuments(accountId);
    }
}
