package com.aml.investigation.kyc.model;

import java.util.List;

/**
 * Raw response shape from the corporate registry integration — not the same type as
 * AgentFindings. This is what the registry actually returns; KycTools decides what to do
 * with it, the model never sees this type directly except through the tool's returned summary.
 */
public record CorporateStructureResult(
        int layers,
        boolean uboDisclosed,
        List<String> jurisdictions
) {
}
