package com.aml.investigation.kyc;

import com.aml.investigation.common.KycInvestigator;
import com.aml.investigation.common.SubAgentResult;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * The adapter side of Phase 2's ports-and-adapters seam: implements the KycInvestigator port
 * that agent-orchestrator depends on, by delegating to the private, in-process KycAgent proxy.
 * A RemoteKycInvestigator (HTTP-backed) implementation would be added alongside this one, not
 * in place of it, once agent-kyc is extracted into its own service — orchestrator code that
 * depends on KycInvestigator never has to change either way.
 *
 * <p>This is also the one place that assembles a full SubAgentResult. KycAgent only ever
 * authors an AgentFindings — status, confidence, findings — because those are the only fields
 * a model can honestly produce. agentName, latency, and timedOut are all things only this
 * adapter is in a position to know, measured or fixed here, never asked of the model.
 */
@Component
public class LocalKycInvestigator implements KycInvestigator {

    private static final String AGENT_NAME = "kyc";

    private final KycAgent kycAgent;

    public LocalKycInvestigator(KycAgent kycAgent) {
        this.kycAgent = kycAgent;
    }

    @Override
    public SubAgentResult investigate(String accountId) {
        Instant start = Instant.now();
        AgentFindings findings = kycAgent.investigate(accountId);
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
