package dev.modelmaestro.orchestration;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.ModelClientFactory;
import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.config.ModelConfigRepository;
import dev.modelmaestro.orchestration.plan.ModelCatalogEntry;
import dev.modelmaestro.orchestration.plan.PlannedTask;

@Service
public class ModelRouter {

    private final ModelConfigRepository repository;
    private final ModelClientFactory clientFactory;

    public ModelRouter(ModelConfigRepository repository, ModelClientFactory clientFactory) {
        this.repository = repository;
        this.clientFactory = clientFactory;
    }

    public AiModelClient route(PlannedTask task, List<ModelCatalogEntry> planningCatalog) {
        Objects.requireNonNull(task, "Planned task is required.");
        Objects.requireNonNull(planningCatalog, "Planning catalog is required.");
        UUID configId = task.assignedModelConfigId();

        boolean offeredToSupervisor = planningCatalog.stream()
                .anyMatch(entry -> entry.configId().equals(configId));
        if (!offeredToSupervisor) {
            throw new ModelRoutingException("Model configuration " + configId
                    + " was not in the planning catalog.");
        }

        // Recheck current database state: the catalog is only a planning snapshot.
        ModelConfig config = repository.findById(configId)
                .orElseThrow(() -> new ModelRoutingException(
                        "Model configuration " + configId + " no longer exists."));
        if (!config.isEnabled()) {
            throw new ModelRoutingException("Model configuration " + configId + " is disabled.");
        }

        return clientFactory.create(config);
    }
}
