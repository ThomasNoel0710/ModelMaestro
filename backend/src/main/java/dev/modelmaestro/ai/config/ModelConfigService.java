package dev.modelmaestro.ai.config;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.modelmaestro.orchestration.plan.ModelCatalogEntry;

@Service
@Transactional(readOnly = true)
public class ModelConfigService {

    private final ModelConfigRepository repository;
    public ModelConfigService(ModelConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ModelConfig createModel(
            String displayName, ModelProtocol protocol, String baseUrl, String modelId,
            int capabilityScore, String description,
            long inputCostMicrosPerMillionTokens, long outputCostMicrosPerMillionTokens,
            boolean enabled) {
        return repository.save(new ModelConfig(
                displayName, protocol, baseUrl, modelId, capabilityScore, description,
                inputCostMicrosPerMillionTokens, outputCostMicrosPerMillionTokens, enabled));
    }

    public List<ModelConfig> listModels() {
        return repository.findAll(Sort.by("createdAt").descending().and(Sort.by("id")));
    }

    public List<ModelCatalogEntry> listEnabledCatalogEntries() {
        return repository.findAllByEnabledTrueOrderByCapabilityScoreDesc().stream()
                .map(model -> new ModelCatalogEntry(
                        model.getId(),
                        model.getDisplayName(),
                        model.getCapabilityScore(),
                        model.getDescription(),
                        model.getInputCostMicrosPerMillionTokens(),
                        model.getOutputCostMicrosPerMillionTokens()))
                .toList();
    }

    @Transactional
    public ModelConfig setEnabled(UUID id, boolean enabled) {
        ModelConfig model = repository.findById(id)
                .orElseThrow(() -> new ModelConfigNotFoundException(id));
        if (enabled) {
            model.enable();
        } else {
            model.disable();
        }
        return model;
    }
}
