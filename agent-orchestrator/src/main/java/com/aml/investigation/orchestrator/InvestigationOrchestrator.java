package com.aml.investigation.orchestrator;

import com.aml.investigation.common.KycInvestigator;
import com.aml.investigation.common.NetworkInvestigator;
import com.aml.investigation.common.SanctionsInvestigator;
import com.aml.investigation.common.SubAgentResult;
import com.aml.investigation.store.FindingData;
import com.aml.investigation.store.InvestigationStore;
import com.aml.investigation.store.SubAgentFindingData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The actual "orchestrator" from Phase 1's design — plain Java, not an AiService. Fans out to
 * the three sub-agent ports in parallel, joins them with per-future timeouts, and either calls
 * OrchestratorAgent to synthesize a real verdict or short-circuits to a Java-constructed
 * ESCALATE_TO_HUMAN if any sub-agent times out. No tool-calling loop of its own — every
 * decision about which sub-agents to invoke was made once, here, in code, not by a model.
 */
@Service
public class InvestigationOrchestrator {

    private final KycInvestigator kycInvestigator;
    private final SanctionsInvestigator sanctionsInvestigator;
    private final NetworkInvestigator networkInvestigator;
    private final OrchestratorAgent orchestratorAgent;
    private final Executor investigationExecutor;
    private final InvestigationStore store;

    private final long kycTimeoutSeconds;
    private final long sanctionsTimeoutSeconds;
    private final long networkTimeoutSeconds;

    public InvestigationOrchestrator(
            KycInvestigator kycInvestigator,
            SanctionsInvestigator sanctionsInvestigator,
            NetworkInvestigator networkInvestigator,
            OrchestratorAgent orchestratorAgent,
            Executor investigationExecutor,
            InvestigationStore store,
            @Value("${aml.orchestrator.timeout.kyc-seconds:30}") long kycTimeoutSeconds,
            @Value("${aml.orchestrator.timeout.sanctions-seconds:30}") long sanctionsTimeoutSeconds,
            @Value("${aml.orchestrator.timeout.network-seconds:60}") long networkTimeoutSeconds) {
        this.kycInvestigator = kycInvestigator;
        this.sanctionsInvestigator = sanctionsInvestigator;
        this.networkInvestigator = networkInvestigator;
        this.orchestratorAgent = orchestratorAgent;
        this.investigationExecutor = investigationExecutor;
        this.store = store;
        this.kycTimeoutSeconds = kycTimeoutSeconds;
        this.sanctionsTimeoutSeconds = sanctionsTimeoutSeconds;
        this.networkTimeoutSeconds = networkTimeoutSeconds;
    }

    /**
     * Fire-and-forget from the caller's perspective — the REST controller calls this and
     * returns 202 immediately without waiting; this method keeps running in the background
     * against investigationExecutor and persists its own result when done.
     */
    public void runInvestigation(UUID investigationId, String accountId) {
        store.markInProgress(investigationId);

        // Different timeout budgets per sub-agent, not one shared value — Network's Cypher
        // traversal is a genuinely different call shape than KYC/Sanctions' REST lookups.
        CompletableFuture<SubAgentResult> kycFuture = CompletableFuture
                .supplyAsync(() -> kycInvestigator.investigate(accountId), investigationExecutor)
                .orTimeout(kycTimeoutSeconds, TimeUnit.SECONDS);

        CompletableFuture<SubAgentResult> sanctionsFuture = CompletableFuture
                .supplyAsync(() -> sanctionsInvestigator.investigate(accountId), investigationExecutor)
                .orTimeout(sanctionsTimeoutSeconds, TimeUnit.SECONDS);

        CompletableFuture<SubAgentResult> networkFuture = CompletableFuture
                .supplyAsync(() -> networkInvestigator.investigate(accountId), investigationExecutor)
                .orTimeout(networkTimeoutSeconds, TimeUnit.SECONDS);

        CompletableFuture.allOf(kycFuture, sanctionsFuture, networkFuture)
                .whenComplete((ignoredResult, throwable) -> {
                    if (throwable != null) {
                        // Any single timeout — regardless of how many of the other two
                        // succeeded — short-circuits here. No LLM call, no partial trust.
                        handleIncompleteInvestigation(investigationId, kycFuture, sanctionsFuture, networkFuture);
                    } else {
                        handleCompleteInvestigation(investigationId, kycFuture.join(), sanctionsFuture.join(), networkFuture.join());
                    }
                });
    }

    private void handleCompleteInvestigation(
            UUID investigationId, SubAgentResult kyc, SubAgentResult sanctions, SubAgentResult network) {

        SarVerdict verdict = orchestratorAgent.synthesize(
                SubAgentResultFormatter.format(kyc),
                SubAgentResultFormatter.format(sanctions),
                SubAgentResultFormatter.format(network)
        );

        store.completeWithVerdict(
                investigationId,
                verdict.verdict().name(),
                verdict.confidence(),
                verdict.reviewNote(),
                verdict.sarNarrative(),
                List.of(toFindingData(kyc), toFindingData(sanctions), toFindingData(network))
        );
    }

    private void handleIncompleteInvestigation(
            UUID investigationId,
            CompletableFuture<SubAgentResult> kycFuture,
            CompletableFuture<SubAgentResult> sanctionsFuture,
            CompletableFuture<SubAgentResult> networkFuture) {

        StringBuilder reviewNote = new StringBuilder(
                "Investigation could not be fully completed automatically. ");
        List<SubAgentFindingData> completedFindings = new ArrayList<>();

        appendOutcome(reviewNote, completedFindings, "kyc", kycFuture);
        appendOutcome(reviewNote, completedFindings, "sanctions", sanctionsFuture);
        appendOutcome(reviewNote, completedFindings, "network", networkFuture);

        reviewNote.append("Manual review required.");

        // confidence = 1.0 here is full confidence in the policy decision to escalate on
        // incomplete data, not a claim about the underlying investigation — this is Java
        // applying a fixed rule, not a model making a judgment call. See Phase 6.
        store.completeWithVerdict(
                investigationId,
                Verdict.ESCALATE_TO_HUMAN.name(),
                1.0,
                reviewNote.toString(),
                null,
                completedFindings
        );
    }

    private void appendOutcome(
            StringBuilder reviewNote,
            List<SubAgentFindingData> completedFindings,
            String agentName,
            CompletableFuture<SubAgentResult> future) {

        if (future.isCompletedExceptionally()) {
            reviewNote.append(agentName)
                    .append(" investigation timed out; synthesis was not attempted for this sub-agent. ");
        } else {
            SubAgentResult result = future.join();
            reviewNote.append(agentName).append(" completed: status=").append(result.status())
                    .append(", confidence=").append(result.confidence()).append(". ");
            completedFindings.add(toFindingData(result));
        }
    }

    private SubAgentFindingData toFindingData(SubAgentResult result) {
        List<FindingData> findings = result.findings().stream()
                .map(finding -> new FindingData(finding.observation(), finding.evidence()))
                .toList();

        return new SubAgentFindingData(
                result.agentName(),
                result.status(),
                result.confidence(),
                findings,
                result.latency().toMillis(),
                result.timedOut()
        );
    }
}
