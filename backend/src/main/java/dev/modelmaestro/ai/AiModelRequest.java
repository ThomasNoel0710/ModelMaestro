package dev.modelmaestro.ai;

/**
 * The provider-independent input sent to an AI model.
 */
public record AiModelRequest(
        String systemPrompt,
        String userPrompt) {
}
