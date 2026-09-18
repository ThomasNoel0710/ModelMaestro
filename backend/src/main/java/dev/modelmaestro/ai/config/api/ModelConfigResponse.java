package dev.modelmaestro.ai.config.api;

import java.time.Instant;
import java.util.UUID;

import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.config.ModelProtocol;

public record ModelConfigResponse(
        UUID id,
        String displayName,
        ModelProtocol protocol,
        String baseUrl,
        String modelId,
        int capabilityScore,
        String description,
        long inputCostMicrosPerMillionTokens,
        long outputCostMicrosPerMillionTokens,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static ModelConfigResponse from(ModelConfig model) {
        return new ModelConfigResponse(
                model.getId(), model.getDisplayName(), model.getProtocol(),
                model.getBaseUrl(), model.getModelId(), model.getCapabilityScore(),
                model.getDescription(), model.getInputCostMicrosPerMillionTokens(),
                model.getOutputCostMicrosPerMillionTokens(), model.isEnabled(),
                model.getCreatedAt(), model.getUpdatedAt());
    }
}
