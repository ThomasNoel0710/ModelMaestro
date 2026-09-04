package dev.modelmaestro.orchestration;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestrationRunRepository
        extends JpaRepository<OrchestrationRun, UUID> {
}
