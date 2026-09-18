package dev.modelmaestro.orchestration.plan;

import java.util.Objects;
import java.util.UUID;

public record PlannedTask(
        String instruction,
        UUID assignedModelConfigId,
        String reason) {

    public PlannedTask {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("Task instruction must not be blank.");
        }
        Objects.requireNonNull(
                assignedModelConfigId,
                "Assigned model configuration ID is required.");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Model assignment reason must not be blank.");
        }

        instruction = instruction.strip();
        reason = reason.strip();
    }
}
