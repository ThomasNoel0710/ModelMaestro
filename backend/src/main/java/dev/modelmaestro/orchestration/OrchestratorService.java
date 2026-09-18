package dev.modelmaestro.orchestration;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.AiModelRequest;
import dev.modelmaestro.ai.AiModelResponse;
import dev.modelmaestro.orchestration.plan.ExecutionPlan;
import dev.modelmaestro.orchestration.plan.ModelCatalogEntry;
import dev.modelmaestro.orchestration.plan.PlanningResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrchestratorService {

    private static final String PLANNER_PROMPT = """
            You are the supervisor. Create a concise plan for the user's objective.
            Assign work using only configuration IDs from the supplied model catalog.
            Consider capability, description, and price when choosing a model.
            Return only valid JSON without Markdown fences, using this exact structure:
            {
              "summary": "short explanation of the overall approach",
              "tasks": [
                {
                  "instruction": "specific work to perform",
                  "assignedModelConfigId": "configuration UUID from the catalog",
                  "reason": "why this model is appropriate"
                }
              ]
            }
            """;
    private static final String WORKER_PROMPT = """
            You are the worker. Complete the assigned task using the supplied plan.
            """;
    private static final String REVIEWER_PROMPT = """
            You are the supervisor. Review the worker result and produce the final answer.
            """;

    private final AiModelClient modelClient;
    private final ObjectMapper objectMapper;

    public OrchestratorService(AiModelClient modelClient, ObjectMapper objectMapper) {
        this.modelClient = modelClient;
        this.objectMapper = objectMapper;
    }

    public OrchestrationResult execute(
            String objective,
            List<ModelCatalogEntry> modelCatalog,
            AiModelClient supervisor) {
        PlanningResult plan = plan(objective, modelCatalog, supervisor);
        AiModelResponse work = work(objective, plan.modelResponse());
        AiModelResponse review = review(objective, work, supervisor);

        return new OrchestrationResult(plan, work, review);
    }

    public PlanningResult plan(
            String objective,
            List<ModelCatalogEntry> modelCatalog,
            AiModelClient supervisor) {
        String normalizedObjective = normalizeObjective(objective);
        String formattedCatalog = formatModelCatalog(modelCatalog);

        AiModelResponse modelResponse = supervisor.generate(new AiModelRequest(
                PLANNER_PROMPT,
                "Objective:\n%s\n\nAvailable models:\n%s"
                        .formatted(normalizedObjective, formattedCatalog)));

        try {
            ExecutionPlan executionPlan = objectMapper.readValue(
                    modelResponse.content(), ExecutionPlan.class);
            return new PlanningResult(modelResponse, executionPlan);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Supervisor returned an invalid execution plan.", exception);
        }
    }

    public AiModelResponse work(String objective, AiModelResponse plan) {
        String normalizedObjective = normalizeObjective(objective);

        return modelClient.generate(new AiModelRequest(
                WORKER_PROMPT,
                "Objective:\n%s\n\nPlan:\n%s".formatted(normalizedObjective, plan.content())));
    }

    public AiModelResponse review(String objective, AiModelResponse work, AiModelClient supervisor) {
        String normalizedObjective = normalizeObjective(objective);

        return supervisor.generate(new AiModelRequest(
                REVIEWER_PROMPT,
                "Objective:\n%s\n\nWorker result:\n%s".formatted(normalizedObjective, work.content())));
    }

    private String normalizeObjective(String objective) {
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("objective must not be blank");
        }
        return objective.strip();
    }

    private String formatModelCatalog(List<ModelCatalogEntry> modelCatalog) {
        if (modelCatalog == null || modelCatalog.isEmpty()) {
            throw new IllegalArgumentException("model catalog must not be empty");
        }

        return modelCatalog.stream()
                .map(model -> """
                        - Configuration ID: %s
                          Name: %s
                          Capability score: %d/10
                          Description: %s
                          Input cost: %d micros per million tokens
                          Output cost: %d micros per million tokens
                        """.formatted(
                                model.configId(),
                                model.displayName(),
                                model.capabilityScore(),
                                model.description() == null ? "Not provided" : model.description(),
                                model.inputCostMicrosPerMillionTokens(),
                                model.outputCostMicrosPerMillionTokens()).strip())
                .collect(Collectors.joining("\n"));
    }
}
