package com.aml.investigation.common;

/**
 * Port for the Network sub-agent. Implemented by a LocalNetworkInvestigator today (delegating
 * to an in-process LangChain4j AiService); a RemoteNetworkInvestigator implementation is added,
 * not substituted for this interface, once agent-network is extracted into its own service.
 */
public interface NetworkInvestigator {

    SubAgentResult investigate(String accountId);
}
