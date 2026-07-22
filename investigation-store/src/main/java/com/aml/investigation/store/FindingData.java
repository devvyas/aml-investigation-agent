package com.aml.investigation.store;

/**
 * Plain data mirroring agent-tools-common's Finding shape — deliberately not importing that
 * type, since investigation-store has no dependency on agent-tools-common. Coincidental shape,
 * not a shared contract; see Phase 4's reasoning on AgentFindings for the same principle.
 */
public record FindingData(String observation, String evidence) {
}
