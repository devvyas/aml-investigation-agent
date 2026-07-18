package com.aml.investigation.common;

/**
 * A single cited observation backing a sub-agent's status classification —
 * what was observed, and the tool output that supports it.
 */
public record Finding(String observation, String evidence) {
}
