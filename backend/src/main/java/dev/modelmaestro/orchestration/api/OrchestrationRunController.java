package dev.modelmaestro.orchestration.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.modelmaestro.orchestration.OrchestrationRun;
import dev.modelmaestro.orchestration.OrchestrationRunService;
import dev.modelmaestro.orchestration.RunExecutionService;

@RestController
@RequestMapping("/api/v1/runs")
public class OrchestrationRunController {

    private final OrchestrationRunService runService;
    private final RunExecutionService runExecutionService;

    public OrchestrationRunController(
            OrchestrationRunService runService,
            RunExecutionService runExecutionService) {
        this.runService = runService;
        this.runExecutionService = runExecutionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse createRun(@Valid @RequestBody CreateRunRequest request) {
        OrchestrationRun run = runService.createRun(
                request.objective(),
                request.budgetMicros(),
                request.supervisorConfigId());
        return RunResponse.from(run);
    }

    @GetMapping("/{id}")
    public RunResponse getRun(@PathVariable UUID id) {
        return RunResponse.from(runService.getRun(id));
    }

    @PostMapping("/{id}/execute")
    public RunResponse executeRun(@PathVariable UUID id) {
        return RunResponse.from(runExecutionService.executeRun(id));
    }
}
