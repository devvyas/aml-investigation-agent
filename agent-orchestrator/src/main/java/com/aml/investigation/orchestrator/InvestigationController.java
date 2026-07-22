package com.aml.investigation.orchestrator;

import com.aml.investigation.store.InvestigationStatus;
import com.aml.investigation.store.InvestigationStore;
import com.aml.investigation.store.InvestigationSummary;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * POST /invoke is asynchronous, not a blocking call — see Phase 8's reasoning. It returns as
 * soon as the investigation is recorded PENDING and the fan-out has been kicked off; the actual
 * verdict is retrieved later through GET /investigations/{id}. Binding the HTTP response to how
 * long an investigation takes would couple this system's behavior to transport-layer timeouts
 * that have nothing to do with the investigation itself.
 */
@RestController
@RequestMapping("/api/v1")
public class InvestigationController {

    private final InvestigationStore store;
    private final InvestigationOrchestrator orchestrator;

    public InvestigationController(InvestigationStore store, InvestigationOrchestrator orchestrator) {
        this.store = store;
        this.orchestrator = orchestrator;
    }

    @PostMapping("/invoke")
    public ResponseEntity<InvokeResponse> invoke(@RequestBody InvokeRequest request) {
        UUID investigationId = store.createPending(request.accountId());

        // Returns almost immediately — this call only sets up the CompletableFuture fan-out
        // and registers a completion callback; the actual sub-agent calls run on
        // investigationExecutor, not this request thread.
        orchestrator.runInvestigation(investigationId, request.accountId());

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/investigations/{id}")
                .buildAndExpand(investigationId)
                .toUri();

        return ResponseEntity.accepted()
                .location(location)
                .body(new InvokeResponse(investigationId, InvestigationStatus.PENDING));
    }

    @GetMapping("/investigations/{id}")
    public ResponseEntity<InvestigationSummary> getInvestigation(@PathVariable UUID id) {
        return store.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
