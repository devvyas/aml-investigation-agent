package com.aml.investigation.store;

import java.util.List;

public record SubAgentFindingData(
        String agentName,
        String status,
        double confidence,
        List<FindingData> findings,
        long latencyMillis,
        boolean timedOut
) {
}
