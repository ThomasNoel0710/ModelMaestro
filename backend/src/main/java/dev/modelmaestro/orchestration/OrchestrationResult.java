package dev.modelmaestro.orchestration;

import dev.modelmaestro.ai.AiModelResponse;

public record OrchestrationResult(
        AiModelResponse plan,
        AiModelResponse work,
        AiModelResponse review) {

    public long totalCostMicros() {
        return plan.costMicros() + work.costMicros() + review.costMicros();
    }
}
