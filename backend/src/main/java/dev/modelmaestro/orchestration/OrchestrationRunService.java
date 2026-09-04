package dev.modelmaestro.orchestration;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrchestrationRunService {

    private final OrchestrationRunRepository runRepository;

    public OrchestrationRunService(OrchestrationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    @Transactional
    public OrchestrationRun createRun(String objective, long budgetMicros) {
        OrchestrationRun run = new OrchestrationRun(objective, budgetMicros);
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
                result.plan().content(),
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
