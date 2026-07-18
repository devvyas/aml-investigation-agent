package com.aml.investigation.kyc;

/**
 * Private to agent-kyc — classifies what was observed, never a risk judgment or a value from
 * the orchestrator's verdict vocabulary. See Phase 4's system-message scoping decision.
 */
public enum KycStatus {
    UBO_DISCLOSED,
    UBO_UNDISCLOSED,
    STRUCTURE_UNRESOLVED
}
