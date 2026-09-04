package dev.modelmaestro.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

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

    @Test
    void createsAndFindsRun() {
        OrchestrationRun created = runService.createRun(
                "  Analyze three electric vehicle companies  ",
                200_000L);

        entityManager.flush();
        entityManager.clear();

        Optional<OrchestrationRun> result = runService.findRun(created.getId());

        assertThat(result).isPresent();

        OrchestrationRun found = result.orElseThrow();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getObjective())
                .isEqualTo("Analyze three electric vehicle companies");
        assertThat(found.getStatus()).isEqualTo(RunStatus.CREATED);
        assertThat(found.getBudgetMicros()).isEqualTo(200_000L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void rejectsBlankObjective() {
        assertThatThrownBy(() -> runService.createRun("   ", 200_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Objective must not be blank.");
    }

    @Test
    void rejectsNonPositiveBudget() {
        assertThatThrownBy(() -> runService.createRun("Analyze competitors", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Budget must be greater than zero.");
    }

    @Test
    void executesRunAndPersistsResultSummary() {
        OrchestrationRun created = runService.createRun(
                "Design a REST API",
                1_000_000L);

        runExecutionService.executeRun(created.getId());
        entityManager.flush();
        entityManager.clear();

        OrchestrationRun executed = runService.getRun(created.getId());
        assertThat(executed.getStatus()).isEqualTo(RunStatus.COMPLETED);
        assertThat(executed.getPlanContent()).contains("Design a REST API");
        assertThat(executed.getWorkContent()).contains(executed.getPlanContent());
        assertThat(executed.getFinalResult()).contains(executed.getWorkContent());
        assertThat(executed.getSpentMicros()).isPositive();
    }
}
