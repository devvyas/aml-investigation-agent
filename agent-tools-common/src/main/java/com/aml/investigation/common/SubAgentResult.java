package com.aml.investigation.common;

import java.time.Duration;
import java.util.List;

/**
 * The result a sub-agent port hands back to the orchestrator.
 *
 * <p>{@code status} is a String, not an enum: each sub-agent module defines its own private
 * status enum (e.g. NetworkStatus.STRUCTURING_DETECTED), and this shared record can't depend on
 * those without breaking the module boundary from agent-tools-common down. The adapter converts
 * its module's enum to its {@code name()} when assembling this record.
 *
 * <p>{@code agentName}, {@code latency}, and {@code timedOut} are never authored by a model —
 * they're assembled by the {@code Local*Investigator} adapter after the AiService call returns
 * (or times out), never part of the AiService's own return type.
 */
public record SubAgentResult(
        String agentName,
        String status,
        double confidence,
        List<Finding> findings,
        Duration latency,
        boolean timedOut
) {
}
