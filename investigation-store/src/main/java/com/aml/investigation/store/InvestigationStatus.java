package com.aml.investigation.store;

/**
 * Job-lifecycle status for an investigation — separate from any sub-agent's own status enum,
 * or the orchestrator's verdict. This tracks the async job itself: has it started, is it
 * still running, is a result available yet.
 */
public enum InvestigationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
