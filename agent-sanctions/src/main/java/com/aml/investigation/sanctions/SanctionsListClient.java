package com.aml.investigation.sanctions;

import com.aml.investigation.sanctions.model.AdverseMediaResult;
import com.aml.investigation.sanctions.model.PepResult;
import com.aml.investigation.sanctions.model.SanctionsMatch;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Owns the integration with the external sanctions/PEP/adverse-media provider(s) — the HTTP
 * calls, retries, and response parsing. Same split as CorporateRegistryClient in agent-kyc:
 * no idea a language model exists anywhere in the system.
 */
@Component
public class SanctionsListClient {

    private final RestClient restClient;

    public SanctionsListClient(@Value("${aml.sanctions.provider.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<SanctionsMatch> batchSanctionsCheck(List<String> entityNames) {
        return restClient.post()
                .uri("/screen/batch")
                .body(entityNames)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SanctionsMatch>>() {
                });
    }

    public PepResult getPepStatus(String personId) {
        return restClient.get()
                .uri("/persons/{personId}/pep-status", personId)
                .retrieve()
                .body(PepResult.class);
    }

    public AdverseMediaResult checkAdverseMedia(String entityId) {
        return restClient.get()
                .uri("/entities/{entityId}/adverse-media", entityId)
                .retrieve()
                .body(AdverseMediaResult.class);
    }
}
