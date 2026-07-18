package com.aml.investigation.kyc;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Private to agent-kyc — never exported. LangChain4j builds a dynamic proxy around this
 * interface via AiServices.builder(); nothing outside this module is allowed to depend on it.
 * agent-orchestrator only ever sees this sub-agent through the KycInvestigator port and
 * SubAgentResult, both from agent-tools-common. See Phase 2's ports-and-adapters seam.
 */
public interface KycAgent {

    @SystemMessage("""
            You are a KYC (Know Your Customer) investigator. Your only job is to establish the
            corporate structure and beneficial ownership facts for the given account — nothing
            more.

            Use your tools to resolve the corporate structure, identify registered agents, and
            verify submitted documents as needed. Cite the specific tool output backing every
            finding you report.

            Classify your findings using exactly one of these statuses:
            - UBO_DISCLOSED: a named ultimate beneficial owner is disclosed in the corporate
              structure.
            - UBO_UNDISCLOSED: no beneficial owner is disclosed, or the disclosed owner cannot
              be verified.
            - STRUCTURE_UNRESOLVED: the corporate structure could not be fully traced.

            Do not assess risk or suspicion, and do not recommend any action. Do not use words
            like "suspicious", "concerning", or "recommend", and never use any of: FILE_SAR,
            CLEAR, ESCALATE_TO_HUMAN, ENHANCED_DUE_DILIGENCE. Report only what you observed and
            the evidence for it.
            """)
    @UserMessage("Investigate the corporate structure and beneficial ownership for account {{accountId}}.")
    AgentFindings investigate(@V("accountId") String accountId);
}
