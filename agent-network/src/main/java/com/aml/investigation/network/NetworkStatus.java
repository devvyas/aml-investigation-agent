package com.aml.investigation.network;

/**
 * Private to agent-network. These three values are what Phase 5's decision rules key off
 * directly as the primary axis of the synthesis table — STRUCTURING_DETECTED is the necessary
 * precondition for FILE_SAR.
 */
public enum NetworkStatus {
    STRUCTURING_DETECTED,
    CIRCULAR_FLOW_DETECTED,
    NO_PATTERN_DETECTED
}
