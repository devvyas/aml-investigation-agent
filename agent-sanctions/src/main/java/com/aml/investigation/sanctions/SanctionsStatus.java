package com.aml.investigation.sanctions;

/**
 * Private to agent-sanctions. MATCH/PARTIAL_MATCH/NO_MATCH are what the orchestrator's Phase 5
 * decision rules key off directly — no PEP-specific or adverse-media-specific status value,
 * since those get folded into the confidence/findings rather than the status itself.
 */
public enum SanctionsStatus {
    MATCH,
    PARTIAL_MATCH,
    NO_MATCH
}
