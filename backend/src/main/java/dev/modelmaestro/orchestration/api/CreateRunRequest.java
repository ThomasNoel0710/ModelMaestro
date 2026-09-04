package dev.modelmaestro.orchestration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateRunRequest(
        @NotBlank(message = "Objective must not be blank.")
        String objective,

        @Positive(message = "Budget must be greater than zero.")
        long budgetMicros) {
}

