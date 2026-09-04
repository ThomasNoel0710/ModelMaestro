package dev.modelmaestro.orchestration;

import org.springframework.stereotype.Service;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.AiModelRequest;
import dev.modelmaestro.ai.AiModelResponse;

@Service
public class OrchestratorService {

    private static final String PLANNER_PROMPT = """
            You are the supervisor. Create a concise plan for the user's objective.
            """;
    private static final String WORKER_PROMPT = """
            You are the worker. Complete the assigned task using the supplied plan.
            """;
    private static final String REVIEWER_PROMPT = """
            You are the supervisor. Review the worker result and produce the final answer.
            """;

    private final AiModelClient modelClient;

    public OrchestratorService(AiModelClient modelClient) {
        this.modelClient = modelClient;
    }

    public OrchestrationResult execute(String objective) {
        AiModelResponse plan = plan(objective);
        AiModelResponse work = work(objective, plan);
        AiModelResponse review = review(objective, work);

        return new OrchestrationResult(plan, work, review);
    }

    public AiModelResponse plan(String objective) {
        String normalizedObjective = normalizeObjective(objective);

        return modelClient.generate(new AiModelRequest(
                PLANNER_PROMPT,
                normalizedObjective));
    }

    public AiModelResponse work(String objective, AiModelResponse plan) {
        String normalizedObjective = normalizeObjective(objective);

        return modelClient.generate(new AiModelRequest(
                WORKER_PROMPT,
                "Objective:\n%s\n\nPlan:\n%s".formatted(normalizedObjective, plan.content())));
    }

    public AiModelResponse review(String objective, AiModelResponse work) {
        String normalizedObjective = normalizeObjective(objective);

        return modelClient.generate(new AiModelRequest(
                REVIEWER_PROMPT,
                "Objective:\n%s\n\nWorker result:\n%s".formatted(normalizedObjective, work.content())));
    }

    private String normalizeObjective(String objective) {
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("objective must not be blank");
        }
        return objective.strip();
    }
}
