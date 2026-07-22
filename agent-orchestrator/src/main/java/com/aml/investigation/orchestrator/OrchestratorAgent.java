package com.aml.investigation.orchestrator;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Private to agent-orchestrator — never exported. Unlike KycAgent/SanctionsAgent/NetworkAgent,
 * this AiService has no tools registered against it at all (see OrchestratorAgentConfig) — a
 * single reasoning pass over evidence that's already been gathered, per Phase 5.
 *
 * <p>The three summaries are pre-formatted plain text, built in Java by SubAgentResultFormatter,
 * not raw SubAgentResult objects — LangChain4j's simple template substitution works on strings,
 * and formatting the evidence clearly is a job for deterministic code, not the model.
 */
public interface OrchestratorAgent {

    @SystemMessage("""
            You are the synthesis authority for an AML investigation. You receive three
            independent findings — one each from a KYC investigator, a Sanctions investigator,
            and a Network investigator. Each finding contains a domain-specific status
            classification, a confidence score, and cited evidence. None of them contains an
            opinion, risk assessment, or recommendation — only observed facts. You must not
            re-investigate, call any tool, or introduce evidence beyond what these three
            findings provide.

            Apply these rules, in order:

            1. FILE_SAR — if the Network finding's status is STRUCTURING_DETECTED, and the
               Sanctions finding's status is MATCH or PARTIAL_MATCH. Exception: if the KYC
               finding's confidence is below 0.6, PARTIAL_MATCH is not sufficient — require
               MATCH.
            2. ENHANCED_DUE_DILIGENCE — if the Network finding's status is NO_PATTERN_DETECTED,
               and the Sanctions finding's status is MATCH.
            3. CLEAR — if the Network finding's status is NO_PATTERN_DETECTED, and the
               Sanctions finding's status is NO_MATCH, and the KYC finding's status is
               UBO_DISCLOSED.
            4. Otherwise — ESCALATE_TO_HUMAN. This is the default for any combination not
               covered by rules 1-3, including partial, weak, or ambiguous signals from any
               investigator.

            Return a verdict (exactly one of the four above), a confidence score from 0 to 1,
            a reviewNote — a concise, factual explanation of the verdict citing the specific
            findings that drove it, written for an internal case file — and a sarNarrative.
            Only populate sarNarrative when the verdict is FILE_SAR, written to formal
            regulatory filing conventions (who, what, when, why suspicious); leave it empty for
            every other verdict.

            Do not hedge. Do not suggest further investigation. Do not state anything that
            contradicts a finding you were given.
            """)
    @UserMessage("""
            KYC investigator findings:
            {{kycSummary}}

            Sanctions investigator findings:
            {{sanctionsSummary}}

            Network investigator findings:
            {{networkSummary}}
            """)
    SarVerdict synthesize(
            @V("kycSummary") String kycSummary,
            @V("sanctionsSummary") String sanctionsSummary,
            @V("networkSummary") String networkSummary
    );
}
