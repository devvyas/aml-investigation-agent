package com.aml.investigation.common;

/**
 * Port for the Sanctions sub-agent. Implemented by a LocalSanctionsInvestigator today
 * (delegating to an in-process LangChain4j AiService); a RemoteSanctionsInvestigator
 * implementation is added, not substituted for this interface, once agent-sanctions is
 * extracted into its own service.
 */
public interface SanctionsInvestigator {

    SubAgentResult investigate(String accountId);
}
