package com.aml.investigation.store;

import com.aml.investigation.store.entity.InvestigationEntity;
import com.aml.investigation.store.entity.InvestigationRepository;
import com.aml.investigation.store.entity.SubAgentFindingEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only public entry point into this module — agent-orchestrator calls this, never the JPA
 * repository or the entities directly. Owns the mapping between plain data (what the
 * orchestrator hands in) and JPA entities (this module's own persistence concern).
 */
@Service
public class InvestigationStore {

    private final InvestigationRepository repository;
    private final ObjectMapper objectMapper;

    public InvestigationStore(InvestigationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID createPending(String accountId) {
        UUID id = UUID.randomUUID();
        InvestigationEntity entity = new InvestigationEntity(id, accountId, InvestigationStatus.PENDING, Instant.now());
        repository.save(entity);
        return id;
    }

    @Transactional
    public void markInProgress(UUID investigationId) {
        InvestigationEntity entity = requireEntity(investigationId);
        entity.setStatus(InvestigationStatus.IN_PROGRESS);
    }

    @Transactional
    public void completeWithVerdict(
            UUID investigationId,
            String verdict,
            double confidence,
            String reviewNote,
            String sarNarrative,
            List<SubAgentFindingData> subAgentFindings
    ) {
        InvestigationEntity entity = requireEntity(investigationId);
        entity.setStatus(InvestigationStatus.COMPLETED);
        entity.setVerdict(verdict);
        entity.setConfidence(confidence);
        entity.setReviewNote(reviewNote);
        entity.setSarNarrative(sarNarrative);
        entity.setCompletedAt(Instant.now());

        for (SubAgentFindingData data : subAgentFindings) {
            entity.addSubAgentFinding(new SubAgentFindingEntity(
                    data.agentName(),
                    data.status(),
                    data.confidence(),
                    writeFindingsJson(data.findings()),
                    data.latencyMillis(),
                    data.timedOut()
            ));
        }
    }

    @Transactional(readOnly = true)
    public Optional<InvestigationSummary> findById(UUID investigationId) {
        return repository.findById(investigationId).map(this::toSummary);
    }

    private InvestigationEntity requireEntity(UUID investigationId) {
        return repository.findById(investigationId)
                .orElseThrow(() -> new IllegalArgumentException("No investigation found for id " + investigationId));
    }

    private InvestigationSummary toSummary(InvestigationEntity entity) {
        List<SubAgentFindingData> findings = entity.getSubAgentFindings().stream()
                .map(f -> new SubAgentFindingData(
                        f.getAgentName(),
                        f.getStatus(),
                        f.getConfidence(),
                        readFindingsJson(f.getFindingsJson()),
                        f.getLatencyMillis(),
                        f.isTimedOut()
                ))
                .toList();

        return new InvestigationSummary(
                entity.getId(),
                entity.getAccountId(),
                entity.getStatus(),
                entity.getVerdict(),
                entity.getConfidence(),
                entity.getReviewNote(),
                entity.getSarNarrative(),
                entity.getSubmittedAt(),
                entity.getCompletedAt(),
                findings
        );
    }

    private String writeFindingsJson(List<FindingData> findings) {
        try {
            return objectMapper.writeValueAsString(findings);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize findings", e);
        }
    }

    private List<FindingData> readFindingsJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<FindingData>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize findings", e);
        }
    }
}
