package dev.modelmaestro.orchestration;

import java.util.Optional;
import java.util.UUID;
import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.config.ModelConfigRepository;
import dev.modelmaestro.ai.config.ModelConfigNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrchestrationRunService {

    private final OrchestrationRunRepository runRepository;
    private final ModelConfigRepository modelRepository;

    public OrchestrationRunService(OrchestrationRunRepository runRepository,
            ModelConfigRepository modelRepository) {
        this.runRepository = runRepository;
        this.modelRepository = modelRepository;
    }

    @Transactional
    public OrchestrationRun createRun(String objective, long budgetMicros, UUID supervisorConfigId) {
        OrchestrationRun run = new OrchestrationRun(objective, budgetMicros, supervisorConfigId);
        ModelConfig supervisor = modelRepository.findById(supervisorConfigId)
                .orElseThrow(() -> new ModelConfigNotFoundException(supervisorConfigId));
        if (!supervisor.isEnabled()) {
            throw new SupervisorUnavailableException(supervisorConfigId);
        }
        return runRepository.save(run);
    }

    public Optional<OrchestrationRun> findRun(UUID id) {
        return runRepository.findById(id);
    }

    public OrchestrationRun getRun(UUID id) {
        return findRun(id)
                .orElseThrow(() -> new RunNotFoundException(id));
    }

    @Transactional
    public OrchestrationRun startPlanning(UUID id) {
        OrchestrationRun run = getRun(id);
        run.startPlanning();
        return run;
    }

    @Transactional
    public OrchestrationRun startRunning(UUID id) {
        OrchestrationRun run = getRun(id);
        run.startRunning();
        return run;
    }

    @Transactional
    public OrchestrationRun startReviewing(UUID id) {
        OrchestrationRun run = getRun(id);
        run.startReviewing();
        return run;
    }

    @Transactional
    public OrchestrationRun complete(UUID id, OrchestrationResult result) {
        OrchestrationRun run = getRun(id);
        run.complete(
                result.plan().modelResponse().content(),
                result.work().content(),
                result.review().content(),
                result.totalCostMicros());
        return run;
    }

    @Transactional
    public OrchestrationRun fail(UUID id) {
        OrchestrationRun run = getRun(id);
        run.fail();
        return run;
    }
}
