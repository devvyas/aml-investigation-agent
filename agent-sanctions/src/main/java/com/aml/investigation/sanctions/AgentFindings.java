package com.aml.investigation.sanctions;

import com.aml.investigation.common.Finding;
import java.util.List;

/**
 * What the SanctionsAgent AiService actually authors. Private to agent-sanctions and
 * deliberately not shared with agent-kyc/agent-network — see Phase 4's "duplicate, don't
 * share" decision in agent-tools-common.
 */
public record AgentFindings(SanctionsStatus status, double confidence, List<Finding> findings) {
}
