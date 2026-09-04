package dev.modelmaestro.ai.fake;

import java.util.Objects;

import org.springframework.stereotype.Component;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.AiModelRequest;
import dev.modelmaestro.ai.AiModelResponse;

@Component
public class FakeAiModelClient implements AiModelClient {

    private static final String MODEL_ID = "fake-model";

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public AiModelResponse generate(AiModelRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String content = "Fake response from %s: %s".formatted(MODEL_ID, request.userPrompt());
        int inputTokens = estimateTokens(request.systemPrompt() + request.userPrompt());
        int outputTokens = estimateTokens(content);
        long costMicros = (long) (inputTokens + outputTokens) * 2;

        return new AiModelResponse(content, inputTokens, outputTokens, costMicros);
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
