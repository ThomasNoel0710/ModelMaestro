package dev.modelmaestro.ai;

import java.util.Objects;

import org.springframework.stereotype.Component;

import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.fake.FakeAiModelClient;

@Component
public class ModelClientFactory {

    /**
     * Simulation only: all protocols currently use a fake client.
     * No network requests are made and configured prices are not billed.
     */
    public AiModelClient create(ModelConfig config) {
        Objects.requireNonNull(config, "Model configuration is required.");
        return new FakeAiModelClient(config.getModelId(),
                "%s [%s] (%s)".formatted(
                        config.getDisplayName(), config.getId(), config.getModelId()));
    }
}
