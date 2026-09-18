package dev.modelmaestro.ai.fake;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.AiModelRequest;
import dev.modelmaestro.ai.AiModelResponse;

@Component
public class FakeAiModelClient implements AiModelClient {

    private static final String STRUCTURED_PLAN_MARKER = "Return only valid JSON";
    private static final Pattern CONFIGURATION_ID_PATTERN = Pattern.compile(
            "Configuration ID: ([0-9a-fA-F-]{36})");
    private static final Pattern OBJECTIVE_PATTERN = Pattern.compile(
            "Objective:\\R(.+?)\\R\\RAvailable models:", Pattern.DOTALL);

    private final String modelId;
    private final String identity;

    public FakeAiModelClient() {
        this.modelId = "fake-model";
        this.identity = "fake-model";
    }

    public FakeAiModelClient(String modelId, String identity) {
        this.modelId = Objects.requireNonNull(modelId);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public AiModelResponse generate(AiModelRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String content = request.systemPrompt().contains(STRUCTURED_PLAN_MARKER)
                ? createPlanningResponse(request.userPrompt())
                : "Fake response from %s: %s".formatted(identity, request.userPrompt());
        int inputTokens = estimateTokens(request.systemPrompt() + request.userPrompt());
        int outputTokens = estimateTokens(content);
        long costMicros = (long) (inputTokens + outputTokens) * 2;

        return new AiModelResponse(content, inputTokens, outputTokens, costMicros);
    }

    private String createPlanningResponse(String userPrompt) {
        Matcher configurationMatcher = CONFIGURATION_ID_PATTERN.matcher(userPrompt);
        Matcher objectiveMatcher = OBJECTIVE_PATTERN.matcher(userPrompt);
        if (!configurationMatcher.find()) {
            throw new IllegalArgumentException(
                    "Fake planning requires at least one model configuration ID.");
        }
        if (!objectiveMatcher.find()) {
            throw new IllegalArgumentException("Fake planning requires an objective.");
        }

        return """
                {
                  "summary": "Simulated plan from %s",
                  "tasks": [
                    {
                      "instruction": "Complete this objective: %s",
                      "assignedModelConfigId": "%s",
                      "reason": "Simulation selects the first enabled model in the catalog."
                    }
                  ]
                }
                """.formatted(
                        escapeJson(identity),
                        escapeJson(objectiveMatcher.group(1).strip()),
                        configurationMatcher.group(1));
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
