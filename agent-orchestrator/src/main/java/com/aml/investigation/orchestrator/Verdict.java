package com.aml.investigation.orchestrator;

/**
 * The one place in this system these four values are allowed to appear as an actual verdict.
 * Sub-agents are explicitly forbidden from using this vocabulary in their own system messages
 * — see KycAgent/SanctionsAgent/NetworkAgent's system messages for the matching prohibition.
 */
public enum Verdict {
    FILE_SAR,
    CLEAR,
    ESCALATE_TO_HUMAN,
    ENHANCED_DUE_DILIGENCE
}
