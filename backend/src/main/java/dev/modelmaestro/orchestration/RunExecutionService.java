package dev.modelmaestro.orchestration;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.modelmaestro.ai.AiModelResponse;
import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.ModelClientFactory;
import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.config.ModelConfigRepository;
import dev.modelmaestro.ai.config.ModelConfigNotFoundException;
import dev.modelmaestro.ai.config.ModelConfigService;
import dev.modelmaestro.orchestration.plan.ModelCatalogEntry;
import dev.modelmaestro.orchestration.plan.PlanningResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RunExecutionService {

    private final OrchestrationRunService runService;
    private final OrchestratorService orchestrator;
    private final ModelConfigRepository modelRepository;
    private final ModelConfigService modelConfigService;
    private final ModelClientFactory clientFactory;

    public RunExecutionService(
            OrchestrationRunService runService,
            OrchestratorService orchestrator,
            ModelConfigRepository modelRepository,
            ModelConfigService modelConfigService,
            ModelClientFactory clientFactory) {
        this.runService = runService;
        this.orchestrator = orchestrator;
        this.modelRepository = modelRepository;
        this.modelConfigService = modelConfigService;
        this.clientFactory = clientFactory;
    }

    public OrchestrationRun executeRun(UUID runId) {
        OrchestrationRun run = runService.getRun(runId);
        UUID configId = run.getSupervisorConfigId();
        if (configId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This legacy run has no supervisor. Create a new run with a supervisor configuration.");
        }
        ModelConfig config = modelRepository.findById(configId)
                .orElseThrow(() -> new ModelConfigNotFoundException(configId));
        if (!config.isEnabled()) {
            throw new SupervisorUnavailableException(configId);
        }
        AiModelClient supervisor = clientFactory.create(config);
        List<ModelCatalogEntry> modelCatalog = modelConfigService.listEnabledCatalogEntries();
        runService.startPlanning(runId);
        String objective = run.getObjective();

        try {
            PlanningResult plan = orchestrator.plan(objective, modelCatalog, supervisor);

            runService.startRunning(runId);
            AiModelResponse work = orchestrator.work(objective, plan.modelResponse());

            runService.startReviewing(runId);
            AiModelResponse review = orchestrator.review(objective, work, supervisor);

            return runService.complete(
                    runId,
                    new OrchestrationResult(plan, work, review));
        } catch (RuntimeException exception) {
            runService.fail(runId);
            throw exception;
        }
    }
}
