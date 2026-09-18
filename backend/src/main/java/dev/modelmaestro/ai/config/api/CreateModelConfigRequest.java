package dev.modelmaestro.ai.config.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import dev.modelmaestro.ai.config.ModelProtocol;

public record CreateModelConfigRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotNull ModelProtocol protocol,
        @NotBlank @Size(max = 2048) String baseUrl,
        @NotBlank @Size(max = 255) String modelId,
        @NotNull @Min(1) @Max(10) Integer capabilityScore,
        String description,
        @NotNull @PositiveOrZero Long inputCostMicrosPerMillionTokens,
        @NotNull @PositiveOrZero Long outputCostMicrosPerMillionTokens,
        @NotNull Boolean enabled) {
}
