package dev.modelmaestro.ai.config.api;

import jakarta.validation.constraints.NotNull;

public record UpdateModelEnabledRequest(@NotNull Boolean enabled) {
}
