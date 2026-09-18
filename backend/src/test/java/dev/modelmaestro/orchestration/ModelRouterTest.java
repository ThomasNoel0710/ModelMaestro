package dev.modelmaestro.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.ModelClientFactory;
import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.config.ModelConfigRepository;
import dev.modelmaestro.ai.config.ModelProtocol;
import dev.modelmaestro.orchestration.plan.ModelCatalogEntry;
import dev.modelmaestro.orchestration.plan.PlannedTask;

class ModelRouterTest {

    private final ModelConfigRepository repository = mock(ModelConfigRepository.class);
    private final ModelClientFactory factory = mock(ModelClientFactory.class);
    private final ModelRouter router = new ModelRouter(repository, factory);

    @Test
    void returnsClientForAssignedModelEvenWhenItIsNotFirstInCatalog() {
        ModelConfig config = model();
        AiModelClient client = mock(AiModelClient.class);
        when(repository.findById(config.getId())).thenReturn(Optional.of(config));
        when(factory.create(config)).thenReturn(client);

        assertThat(router.route(task(config.getId()),
                List.of(entry(UUID.randomUUID()), entry(config.getId())))).isSameAs(client);
        verify(factory).create(config);
    }

    @Test
    void rejectsAssignmentOutsideCatalogBeforeDatabaseLookup() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> router.route(task(id), List.of(entry(UUID.randomUUID()))))
                .isInstanceOf(ModelRoutingException.class)
                .hasMessageContaining("not in the planning catalog");
        verifyNoInteractions(repository, factory);
    }

    @Test
    void rejectsConfigurationRemovedSincePlanning() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> router.route(task(id), List.of(entry(id))))
                .isInstanceOf(ModelRoutingException.class)
                .hasMessageContaining("no longer exists");
        verifyNoInteractions(factory);
    }

    @Test
    void rejectsConfigurationDisabledSincePlanning() {
        ModelConfig config = model();
        List<ModelCatalogEntry> catalog = List.of(entry(config.getId()));
        config.disable();
        when(repository.findById(config.getId())).thenReturn(Optional.of(config));
        assertThatThrownBy(() -> router.route(task(config.getId()), catalog))
                .isInstanceOf(ModelRoutingException.class)
                .hasMessageContaining("is disabled");
        verifyNoInteractions(factory);
    }

    private ModelConfig model() {
        return new ModelConfig("Worker", ModelProtocol.OPENAI_COMPATIBLE,
                "https://example.test/v1", "worker-model", 7, null, 100, 200, true);
    }

    private PlannedTask task(UUID id) {
        return new PlannedTask("Write a summary", id, "Suitable for summarization");
    }

    private ModelCatalogEntry entry(UUID id) {
        return new ModelCatalogEntry(id, "Worker", 7, null, 100, 200);
    }
}
