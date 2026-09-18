package dev.modelmaestro.orchestration.plan;

import java.util.Objects;
import java.util.UUID;

public record ModelCatalogEntry(
        UUID configId,
        String displayName,
        int capabilityScore,
        String description,
        long inputCostMicrosPerMillionTokens,
        long outputCostMicrosPerMillionTokens) {

    public ModelCatalogEntry {
        Objects.requireNonNull(configId, "Model configuration ID is required.");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Model display name must not be blank.");
        }
        if (capabilityScore < 1 || capabilityScore > 10) {
            throw new IllegalArgumentException("Capability score must be between 1 and 10.");
        }
        if (inputCostMicrosPerMillionTokens < 0 || outputCostMicrosPerMillionTokens < 0) {
            throw new IllegalArgumentException("Model costs must not be negative.");
        }

        displayName = displayName.strip();
        description = description == null || description.isBlank()
                ? null
                : description.strip();
    }
}
