package dev.modelmaestro.orchestration;

import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.modelmaestro.ai.AiModelResponse;

@Service
public class RunExecutionService {

    private final OrchestrationRunService runService;
    private final OrchestratorService orchestrator;

    public RunExecutionService(
            OrchestrationRunService runService,
            OrchestratorService orchestrator) {
        this.runService = runService;
        this.orchestrator = orchestrator;
    }

    public OrchestrationRun executeRun(UUID runId) {
        OrchestrationRun run = runService.startPlanning(runId);
        String objective = run.getObjective();

        try {
            AiModelResponse plan = orchestrator.plan(objective);

            runService.startRunning(runId);
            AiModelResponse work = orchestrator.work(objective, plan);

            runService.startReviewing(runId);
            AiModelResponse review = orchestrator.review(objective, work);

            return runService.complete(
                    runId,
                    new OrchestrationResult(plan, work, review));
        } catch (RuntimeException exception) {
            runService.fail(runId);
            throw exception;
        }
    }
}
