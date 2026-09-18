package dev.modelmaestro.orchestration;

import dev.modelmaestro.ai.AiModelResponse;
import dev.modelmaestro.orchestration.plan.PlanningResult;

public record OrchestrationResult(
        PlanningResult plan,
        AiModelResponse work,
        AiModelResponse review) {

    public long totalCostMicros() {
        return plan.modelResponse().costMicros() + work.costMicros() + review.costMicros();
    }
}
