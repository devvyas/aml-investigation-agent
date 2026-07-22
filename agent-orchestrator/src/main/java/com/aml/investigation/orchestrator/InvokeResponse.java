package com.aml.investigation.orchestrator;

import com.aml.investigation.store.InvestigationStatus;
import java.util.UUID;

public record InvokeResponse(UUID investigationId, InvestigationStatus status) {
}
