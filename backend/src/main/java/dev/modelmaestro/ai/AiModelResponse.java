package dev.modelmaestro.ai;

/**
 * The provider-independent result returned by an AI model.
 */
public record AiModelResponse(
        String content,
        int inputTokens,
        int outputTokens,
        long costMicros) {
}
