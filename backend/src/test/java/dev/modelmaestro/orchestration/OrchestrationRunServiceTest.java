package dev.modelmaestro.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import dev.modelmaestro.ai.config.ModelConfig;
import dev.modelmaestro.ai.config.ModelConfigRepository;
import dev.modelmaestro.ai.config.ModelConfigNotFoundException;
import dev.modelmaestro.ai.config.ModelProtocol;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrchestrationRunServiceTest {

    @Autowired
    private OrchestrationRunService runService;

    @Autowired
    private RunExecutionService runExecutionService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ModelConfigRepository modelRepository;

    @Autowired
    private OrchestrationRunRepository runRepository;

    private UUID supervisorId;

    @BeforeEach
    void createSupervisor() {
        supervisorId = modelRepository.save(new ModelConfig(
                "Supervisor", ModelProtocol.OPENAI_COMPATIBLE,
                "https://example.test/v1", "example-model", 9, null,
                100L, 200L, true)).getId();
    }

    @Test
    void createsAndFindsRun() {
        OrchestrationRun created = runService.createRun(
                "  Analyze three electric vehicle companies  ",
                200_000L, supervisorId);

        entityManager.flush();
        entityManager.clear();

        Optional<OrchestrationRun> result = runService.findRun(created.getId());

        assertThat(result).isPresent();

        OrchestrationRun found = result.orElseThrow();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getSupervisorConfigId()).isEqualTo(supervisorId);
        assertThat(found.getObjective())
                .isEqualTo("Analyze three electric vehicle companies");
        assertThat(found.getStatus()).isEqualTo(RunStatus.CREATED);
        assertThat(found.getBudgetMicros()).isEqualTo(200_000L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void rejectsBlankObjective() {
        assertThatThrownBy(() -> runService.createRun("   ", 200_000L, supervisorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Objective must not be blank.");
    }

    @Test
    void rejectsNonPositiveBudget() {
        assertThatThrownBy(() -> runService.createRun("Analyze competitors", 0L, supervisorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Budget must be greater than zero.");
    }

    @Test
    void executesRunAndPersistsResultSummary() {
        ModelConfig disabledWorker = modelRepository.save(new ModelConfig(
                "Disabled Worker", ModelProtocol.OPENAI_COMPATIBLE,
                "https://disabled.example.test/v1", "disabled-model", 10,
                "Must not be offered to the supervisor",
                10L, 20L, false));
        OrchestrationRun created = runService.createRun(
                "Design a REST API",
                1_000_000L, supervisorId);

        runExecutionService.executeRun(created.getId());
        entityManager.flush();
        entityManager.clear();

        OrchestrationRun executed = runService.getRun(created.getId());
        assertThat(executed.getStatus()).isEqualTo(RunStatus.COMPLETED);
        assertThat(executed.getPlanContent()).contains("Design a REST API");
        assertThat(executed.getPlanContent())
                .contains(
                        "\"summary\"",
                        "Simulated plan from Supervisor [" + supervisorId + "]",
                        "\"assignedModelConfigId\": \"" + supervisorId + "\"",
                        supervisorId.toString(),
                        "Complete this objective: Design a REST API")
                .doesNotContain(disabledWorker.getId().toString(), "Disabled Worker");
        assertThat(executed.getFinalResult()).startsWith("Fake response from Supervisor [" + supervisorId + "]");
        assertThat(executed.getWorkContent()).startsWith("Fake response from fake-model:");
        assertThat(executed.getWorkContent()).contains(executed.getPlanContent());
        assertThat(executed.getFinalResult()).contains(executed.getWorkContent());
        assertThat(executed.getSpentMicros()).isPositive();
    }

    @Test
    void rejectsUnknownDisabledAndMissingSupervisorWithoutCreatingRun() {
        long count = runRepository.count();
        assertThatThrownBy(() -> runService.createRun("Analyze", 1000L, UUID.randomUUID()))
                .isInstanceOf(ModelConfigNotFoundException.class);
        modelRepository.findById(supervisorId).orElseThrow().disable();
        assertThatThrownBy(() -> runService.createRun("Analyze", 1000L, supervisorId))
                .isInstanceOf(SupervisorUnavailableException.class);
        assertThatThrownBy(() -> runService.createRun("Analyze", 1000L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(runRepository.count()).isEqualTo(count);
    }

    @Test
    void usesDifferentSupervisorConfigurationsForDifferentRuns() {
        UUID otherId = modelRepository.save(new ModelConfig(
                "Other Supervisor", ModelProtocol.OPENAI_COMPATIBLE,
                "https://other.example.test/v1", "example-model", 8, null,
                100L, 200L, true)).getId();
        OrchestrationRun first = runService.createRun("Analyze", 1_000_000L, supervisorId);
        OrchestrationRun second = runService.createRun("Analyze", 1_000_000L, otherId);
        OrchestrationRun resultA = runExecutionService.executeRun(first.getId());
        OrchestrationRun resultB = runExecutionService.executeRun(second.getId());
        assertThat(resultA.getPlanContent()).contains("Simulated plan from Supervisor [" + supervisorId + "]");
        assertThat(resultB.getPlanContent()).contains("Simulated plan from Other Supervisor [" + otherId + "]");
        assertThat(resultB.getFinalResult()).startsWith("Fake response from Other Supervisor [" + otherId + "]");
    }

    @Test
    void rejectsSupervisorDisabledAfterCreationBeforeStartingRun() {
        OrchestrationRun run = runService.createRun("Analyze", 1_000_000L, supervisorId);
        modelRepository.findById(supervisorId).orElseThrow().disable();
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> runExecutionService.executeRun(run.getId()))
                .isInstanceOf(SupervisorUnavailableException.class);
        assertThat(runService.getRun(run.getId()).getStatus()).isEqualTo(RunStatus.CREATED);
        assertThat(runService.getRun(run.getId()).getSpentMicros()).isZero();
    }
}
