package com.aml.investigation.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One sub-agent's result, persisted alongside the final verdict for audit purposes — see
 * Phase 8's decision to persist all three SubAgentResults, not just the verdict.
 *
 * <p>findingsJson is a JSON-serialized blob, not a reference to agent-tools-common's Finding
 * record: investigation-store has no dependency on agent-tools-common at all, matching how it
 * has no dependency on agent-orchestrator either. agent-orchestrator does the mapping (and the
 * JSON serialization) when it calls into this module.
 */
@Entity
@Table(name = "sub_agent_findings")
public class SubAgentFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "investigation_id", nullable = false)
    private InvestigationEntity investigation;

    @Column(nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private double confidence;

    @Column(columnDefinition = "TEXT")
    private String findingsJson;

    @Column(nullable = false)
    private long latencyMillis;

    @Column(nullable = false)
    private boolean timedOut;

    protected SubAgentFindingEntity() {
        // required by JPA
    }

    public SubAgentFindingEntity(String agentName, String status, double confidence,
                                  String findingsJson, long latencyMillis, boolean timedOut) {
        this.agentName = agentName;
        this.status = status;
        this.confidence = confidence;
        this.findingsJson = findingsJson;
        this.latencyMillis = latencyMillis;
        this.timedOut = timedOut;
    }

    public String getId() {
        return id;
    }

    public InvestigationEntity getInvestigation() {
        return investigation;
    }

    public void setInvestigation(InvestigationEntity investigation) {
        this.investigation = investigation;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getStatus() {
        return status;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getFindingsJson() {
        return findingsJson;
    }

    public long getLatencyMillis() {
        return latencyMillis;
    }

    public boolean isTimedOut() {
        return timedOut;
    }
}
