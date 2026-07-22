package com.aml.investigation.orchestrator;

import com.aml.investigation.common.Finding;
import com.aml.investigation.common.SubAgentResult;

/**
 * Formats a SubAgentResult into plain text for OrchestratorAgent's @UserMessage template.
 * Deliberately dumb — no interpretation, no summarizing, just laying out exactly what each
 * sub-agent reported so the model reasons over the real evidence, not a paraphrase of it.
 */
final class SubAgentResultFormatter {

    private SubAgentResultFormatter() {
    }

    static String format(SubAgentResult result) {
        StringBuilder text = new StringBuilder();
        text.append("Status: ").append(result.status()).append('\n');
        text.append("Confidence: ").append(result.confidence()).append('\n');
        text.append("Findings:\n");

        for (Finding finding : result.findings()) {
            text.append("- ").append(finding.observation())
                    .append(" (evidence: ").append(finding.evidence()).append(")\n");
        }

        return text.toString();
    }
}
