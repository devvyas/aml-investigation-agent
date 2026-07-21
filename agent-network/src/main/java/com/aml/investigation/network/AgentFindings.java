package com.aml.investigation.network;

import com.aml.investigation.common.Finding;
import java.util.List;

/**
 * What the NetworkAgent AiService actually authors. Private to agent-network — see Phase 4's
 * "duplicate, don't share" decision in agent-tools-common.
 */
public record AgentFindings(NetworkStatus status, double confidence, List<Finding> findings) {
}
