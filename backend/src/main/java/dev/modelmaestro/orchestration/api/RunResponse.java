package dev.modelmaestro.orchestration.api;

import java.time.Instant;
import java.util.UUID;

import dev.modelmaestro.orchestration.OrchestrationRun;
import dev.modelmaestro.orchestration.RunStatus;

public record RunResponse(
        UUID id,
        String objective,
        RunStatus status,
        long budgetMicros,
        UUID supervisorConfigId,
        long spentMicros,
        String plan,
        String work,
        String finalResult,
        Instant createdAt,
        Instant updatedAt) {

    public static RunResponse from(OrchestrationRun run) {
        return new RunResponse(
                run.getId(),
                run.getObjective(),
                run.getStatus(),
                run.getBudgetMicros(),
                run.getSupervisorConfigId(),
                run.getSpentMicros(),
                run.getPlanContent(),
                run.getWorkContent(),
                run.getFinalResult(),
                run.getCreatedAt(),
                run.getUpdatedAt());
    }
}
