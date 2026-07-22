CREATE TABLE investigations (
    id UUID PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    verdict VARCHAR(64),
    confidence DOUBLE PRECISION,
    review_note TEXT,
    sar_narrative TEXT,
    submitted_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE sub_agent_findings (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigations (id),
    agent_name VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    findings_json TEXT,
    latency_millis BIGINT NOT NULL,
    timed_out BOOLEAN NOT NULL
);

CREATE INDEX idx_sub_agent_findings_investigation_id ON sub_agent_findings (investigation_id);
