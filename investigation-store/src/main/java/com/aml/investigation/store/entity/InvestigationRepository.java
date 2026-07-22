package com.aml.investigation.store.entity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigationRepository extends JpaRepository<InvestigationEntity, UUID> {
}
