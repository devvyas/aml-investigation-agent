package com.aml.investigation.sanctions;

import com.aml.investigation.common.SanctionsInvestigator;
import com.aml.investigation.common.SubAgentResult;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Adapter side of the seam for Sanctions — identical shape to LocalKycInvestigator. See that
 * class's Javadoc for the full reasoning on why agentName/latency/timedOut are assembled here
 * rather than authored by the model.
 */
@Component
public class LocalSanctionsInvestigator implements SanctionsInvestigator {

    private static final String AGENT_NAME = "sanctions";

    private final SanctionsAgent sanctionsAgent;

    public LocalSanctionsInvestigator(SanctionsAgent sanctionsAgent) {
        this.sanctionsAgent = sanctionsAgent;
    }

    @Override
    public SubAgentResult investigate(String accountId) {
        Instant start = Instant.now();
        AgentFindings findings = sanctionsAgent.investigate(accountId);
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
