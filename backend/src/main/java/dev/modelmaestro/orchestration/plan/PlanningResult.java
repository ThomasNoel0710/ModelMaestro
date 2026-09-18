package dev.modelmaestro.orchestration.plan;

import java.util.Objects;

import dev.modelmaestro.ai.AiModelResponse;

public record PlanningResult(
        AiModelResponse modelResponse,
        ExecutionPlan executionPlan) {

    public PlanningResult {
        Objects.requireNonNull(modelResponse, "Planning model response is required.");
        Objects.requireNonNull(executionPlan, "Execution plan is required.");
    }
}
