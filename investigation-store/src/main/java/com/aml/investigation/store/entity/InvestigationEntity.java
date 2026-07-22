package com.aml.investigation.store.entity;

import com.aml.investigation.store.InvestigationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity — a persistence-layer type, not the same as SarVerdict. A model authors
 * SarVerdict's fields; nothing here is model-authored. verdict is a plain String for the same
 * reason SubAgentResult.status is a String: this module has no dependency on agent-orchestrator,
 * so it can't reference whatever Verdict enum lives there.
 */
@Entity
@Table(name = "investigations")
public class InvestigationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigationStatus status;

    private String verdict;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @Column(columnDefinition = "TEXT")
    private String sarNarrative;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant completedAt;

    @OneToMany(mappedBy = "investigation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubAgentFindingEntity> subAgentFindings = new ArrayList<>();

    protected InvestigationEntity() {
        // required by JPA
    }

    public InvestigationEntity(UUID id, String accountId, InvestigationStatus status, Instant submittedAt) {
        this.id = id;
        this.accountId = accountId;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public InvestigationStatus getStatus() {
        return status;
    }

    public void setStatus(InvestigationStatus status) {
        this.status = status;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getSarNarrative() {
        return sarNarrative;
    }

    public void setSarNarrative(String sarNarrative) {
        this.sarNarrative = sarNarrative;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<SubAgentFindingEntity> getSubAgentFindings() {
        return subAgentFindings;
    }

    public void addSubAgentFinding(SubAgentFindingEntity finding) {
        finding.setInvestigation(this);
        this.subAgentFindings.add(finding);
    }
}
