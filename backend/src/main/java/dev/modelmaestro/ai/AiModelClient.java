package dev.modelmaestro.ai;

/**
 * Common boundary for every model provider used by ModelMaestro.
 */
public interface AiModelClient {

    String modelId();

    AiModelResponse generate(AiModelRequest request);
}
