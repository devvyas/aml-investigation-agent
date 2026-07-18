package com.aml.investigation.kyc;

import com.aml.investigation.kyc.model.CorporateStructureResult;
import com.aml.investigation.kyc.model.DocumentVerificationResult;
import com.aml.investigation.kyc.model.RegisteredAgent;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Owns the actual integration with the external corporate registry — the HTTP call, retries,
 * and response parsing. Deliberately has no idea a language model exists anywhere in the
 * system; see Phase 3's reasoning on why this is split out from KycTools.
 */
@Component
public class CorporateRegistryClient {

    private final RestClient restClient;

    public CorporateRegistryClient(@Value("${aml.kyc.registry.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public CorporateStructureResult resolveCorporateStructure(String accountId) {
        return restClient.get()
                .uri("/accounts/{accountId}/structure", accountId)
                .retrieve()
                .body(CorporateStructureResult.class);
    }

    public List<RegisteredAgent> getRegisteredAgents(String entityId) {
        return restClient.get()
                .uri("/entities/{entityId}/registered-agents", entityId)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<RegisteredAgent>>() {
                });
    }

    public DocumentVerificationResult verifyDocuments(String accountId) {
        return restClient.get()
                .uri("/accounts/{accountId}/documents/verification", accountId)
                .retrieve()
                .body(DocumentVerificationResult.class);
    }
}
