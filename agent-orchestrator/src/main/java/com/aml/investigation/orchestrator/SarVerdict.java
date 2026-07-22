package com.aml.investigation.orchestrator;

/**
 * What OrchestratorAgent actually authors. reviewNote is always populated; sarNarrative is
 * populated only when verdict is FILE_SAR and left empty otherwise — not a workaround, since
 * when no SAR is being filed there genuinely is no SAR narrative to write. See Phase 5's
 * reasoning for why this needed two fields instead of one.
 */
public record SarVerdict(Verdict verdict, double confidence, String reviewNote, String sarNarrative) {
}
