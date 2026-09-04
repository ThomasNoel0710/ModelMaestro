package dev.modelmaestro.ai.config;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConfigRepository extends JpaRepository<ModelConfig, UUID> {

    List<ModelConfig> findAllByEnabledTrueOrderByCapabilityScoreDesc();
}
