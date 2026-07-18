package com.aml.investigation.common;

/**
 * Port for the KYC sub-agent. Implemented by a LocalKycInvestigator today (delegating to an
 * in-process LangChain4j AiService); a RemoteKycInvestigator implementation is added, not
 * substituted for this interface, once agent-kyc is extracted into its own service.
 */
public interface KycInvestigator {

    SubAgentResult investigate(String accountId);
}
