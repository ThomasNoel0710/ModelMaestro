package dev.modelmaestro.ai.config;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "model_configs")
public class ModelConfig {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModelProtocol protocol;

    @Column(name = "base_url", nullable = false, length = 2048)
    private String baseUrl;

    @Column(name = "model_id", nullable = false, length = 255)
    private String modelId;

    @Column(name = "capability_score", nullable = false)
    private int capabilityScore;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "input_cost_micros_per_million_tokens", nullable = false)
    private long inputCostMicrosPerMillionTokens;

    @Column(name = "output_cost_micros_per_million_tokens", nullable = false)
    private long outputCostMicrosPerMillionTokens;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ModelConfig() {
        // Required by JPA.
    }

    public ModelConfig(
            String displayName,
            ModelProtocol protocol,
            String baseUrl,
            String modelId,
            int capabilityScore,
            String description,
            long inputCostMicrosPerMillionTokens,
            long outputCostMicrosPerMillionTokens,
            boolean enabled) {
        this.displayName = requireText(displayName, "Display name");
        this.protocol = requireProtocol(protocol);
        this.baseUrl = requireText(baseUrl, "Base URL");
        this.modelId = requireText(modelId, "Model ID");
        this.capabilityScore = requireCapabilityScore(capabilityScore);
        this.description = normalizeDescription(description);
        this.inputCostMicrosPerMillionTokens = requireNonNegative(
                inputCostMicrosPerMillionTokens,
                "Input cost");
        this.outputCostMicrosPerMillionTokens = requireNonNegative(
                outputCostMicrosPerMillionTokens,
                "Output cost");
        this.enabled = enabled;

        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.strip();
    }

    private static ModelProtocol requireProtocol(ModelProtocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("Protocol must not be null.");
        }
        return protocol;
    }

    private static int requireCapabilityScore(int capabilityScore) {
        if (capabilityScore < 1 || capabilityScore > 10) {
            throw new IllegalArgumentException("Capability score must be between 1 and 10.");
        }
        return capabilityScore;
    }

    private static long requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative.");
        }
        return value;
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.strip();
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ModelProtocol getProtocol() {
        return protocol;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelId() {
        return modelId;
    }

    public int getCapabilityScore() {
        return capabilityScore;
    }

    public String getDescription() {
        return description;
    }

    public long getInputCostMicrosPerMillionTokens() {
        return inputCostMicrosPerMillionTokens;
    }

    public long getOutputCostMicrosPerMillionTokens() {
        return outputCostMicrosPerMillionTokens;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
