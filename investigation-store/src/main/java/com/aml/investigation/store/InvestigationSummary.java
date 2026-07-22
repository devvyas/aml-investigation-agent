package com.aml.investigation.store;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What GET /api/v1/investigations/{id} in agent-orchestrator will eventually return — the
 * read-side view assembled from InvestigationEntity, never the entity itself leaking out of
 * this module.
 */
public record InvestigationSummary(
        UUID id,
        String accountId,
        InvestigationStatus status,
        String verdict,
        Double confidence,
        String reviewNote,
        String sarNarrative,
        Instant submittedAt,
        Instant completedAt,
        List<SubAgentFindingData> subAgentFindings
) {
}
