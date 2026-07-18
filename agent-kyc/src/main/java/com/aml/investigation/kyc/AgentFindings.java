package com.aml.investigation.kyc;

import com.aml.investigation.common.Finding;
import java.util.List;

/**
 * What the KycAgent AiService actually authors — nothing else. No agentName, latency, or
 * timedOut here; those aren't things a model can honestly produce. See Phase 4's reasoning.
 *
 * <p>Private to agent-kyc and deliberately not shared with agent-sanctions/agent-network even
 * though the shape is identical today — see Phase 4's "duplicate, don't share" decision.
 */
public record AgentFindings(KycStatus status, double confidence, List<Finding> findings) {
}
