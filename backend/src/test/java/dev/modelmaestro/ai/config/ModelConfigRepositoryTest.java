package dev.modelmaestro.ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModelConfigRepositoryTest {

    @Autowired
    private ModelConfigRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesConfigurationAndFindsOnlyEnabledModels() {
        ModelConfig enabled = repository.save(model("Primary Worker", 7, true));
        repository.save(model("Disabled Worker", 9, false));

        entityManager.flush();
        entityManager.clear();

        List<ModelConfig> result = repository.findAllByEnabledTrueOrderByCapabilityScoreDesc();

        assertThat(result).extracting(ModelConfig::getId).containsExactly(enabled.getId());
        ModelConfig found = result.getFirst();
        assertThat(found.getDisplayName()).isEqualTo("Primary Worker");
        assertThat(found.getProtocol()).isEqualTo(ModelProtocol.OPENAI_COMPATIBLE);
        assertThat(found.getModelId()).isEqualTo("example-model");
        assertThat(found.getCapabilityScore()).isEqualTo(7);
        assertThat(found.getDescription()).isEqualTo("Good at coding");
        assertThat(found.getInputCostMicrosPerMillionTokens()).isEqualTo(100_000L);
        assertThat(found.getOutputCostMicrosPerMillionTokens()).isEqualTo(200_000L);
    }

    @Test
    void rejectsCapabilityScoreOutsideOneToTen() {
        assertThatThrownBy(() -> model("Invalid Model", 11, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Capability score must be between 1 and 10.");
    }

    private ModelConfig model(String displayName, int capabilityScore, boolean enabled) {
        return new ModelConfig(
                displayName,
                ModelProtocol.OPENAI_COMPATIBLE,
                "https://api.example.test/v1",
                "example-model",
                capabilityScore,
                "  Good at coding  ",
                100_000L,
                200_000L,
                enabled);
    }
}
