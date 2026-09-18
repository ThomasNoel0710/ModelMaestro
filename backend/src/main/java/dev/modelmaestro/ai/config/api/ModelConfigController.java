package dev.modelmaestro.ai.config.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.modelmaestro.ai.config.ModelConfigService;

@RestController
@RequestMapping("/api/v1/models")
public class ModelConfigController {

    private final ModelConfigService service;

    public ModelConfigController(ModelConfigService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModelConfigResponse createModel(@Valid @RequestBody CreateModelConfigRequest request) {
        return ModelConfigResponse.from(service.createModel(
                request.displayName(), request.protocol(), request.baseUrl(), request.modelId(),
                request.capabilityScore(), request.description(),
                request.inputCostMicrosPerMillionTokens(), request.outputCostMicrosPerMillionTokens(),
                request.enabled()));
    }

    @GetMapping
    public List<ModelConfigResponse> listModels() {
        return service.listModels().stream().map(ModelConfigResponse::from).toList();
    }

    @PatchMapping("/{id}/enabled")
    public ModelConfigResponse setEnabled(@PathVariable UUID id,
            @Valid @RequestBody UpdateModelEnabledRequest request) {
        return ModelConfigResponse.from(service.setEnabled(id, request.enabled()));
    }
}
