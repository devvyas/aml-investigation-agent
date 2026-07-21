package com.aml.investigation.network;

import com.aml.investigation.common.NetworkInvestigator;
import com.aml.investigation.common.SubAgentResult;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Adapter side of the seam for Network — identical shape to LocalKycInvestigator and
 * LocalSanctionsInvestigator.
 */
@Component
public class LocalNetworkInvestigator implements NetworkInvestigator {

    private static final String AGENT_NAME = "network";

    private final NetworkAgent networkAgent;

    public LocalNetworkInvestigator(NetworkAgent networkAgent) {
        this.networkAgent = networkAgent;
    }

    @Override
    public SubAgentResult investigate(String accountId) {
        Instant start = Instant.now();
        AgentFindings findings = networkAgent.investigate(accountId);
        Duration latency = Duration.between(start, Instant.now());

        return new SubAgentResult(
                AGENT_NAME,
                findings.status().name(),
                findings.confidence(),
                findings.findings(),
                latency,
                false
        );
    }
}
