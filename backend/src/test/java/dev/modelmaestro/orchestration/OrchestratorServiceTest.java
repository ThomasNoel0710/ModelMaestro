package dev.modelmaestro.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.AiModelRequest;
import dev.modelmaestro.ai.AiModelResponse;
import dev.modelmaestro.orchestration.plan.ModelCatalogEntry;
import tools.jackson.databind.ObjectMapper;

class OrchestratorServiceTest {

    @Test
    void executesPlanningWorkAndReviewInOrder() {
        AiModelClient modelClient = mock(AiModelClient.class);
        AiModelClient supervisor = mock(AiModelClient.class);
        UUID workerConfigId = UUID.randomUUID();
        AiModelResponse plan = new AiModelResponse("""
                {
                  "summary": "Break the objective into one task",
                  "tasks": [{
                    "instruction": "Design the REST API",
                    "assignedModelConfigId": "%s",
                    "reason": "The selected model is suitable for routine implementation."
                  }]
                }
                """.formatted(workerConfigId), 10, 20, 100L);
        AiModelResponse work = new AiModelResponse("Completed worker result", 30, 40, 200L);
        AiModelResponse review = new AiModelResponse("Approved final answer", 50, 60, 300L);
        when(modelClient.generate(any(AiModelRequest.class)))
                .thenReturn(work);
        when(supervisor.generate(any(AiModelRequest.class))).thenReturn(plan, review);

        OrchestratorService orchestrator = new OrchestratorService(modelClient, new ObjectMapper());
        List<ModelCatalogEntry> modelCatalog = List.of(new ModelCatalogEntry(
                workerConfigId,
                "Budget Worker",
                7,
                "Good for routine implementation work",
                100L,
                200L));

        OrchestrationResult result = orchestrator.execute(
                "  Design a REST API  ", modelCatalog, supervisor);

        ArgumentCaptor<AiModelRequest> requestCaptor = ArgumentCaptor.forClass(AiModelRequest.class);
        var order = org.mockito.Mockito.inOrder(supervisor, modelClient);
        order.verify(supervisor).generate(requestCaptor.capture());
        order.verify(modelClient).generate(requestCaptor.capture());
        order.verify(supervisor).generate(requestCaptor.capture());
        org.mockito.Mockito.verifyNoMoreInteractions(supervisor, modelClient);
        List<AiModelRequest> requests = requestCaptor.getAllValues();

        assertThat(requests.get(0).systemPrompt()).contains("supervisor", "plan");
        assertThat(requests.get(0).userPrompt())
                .contains(
                        "Objective:\nDesign a REST API",
                        workerConfigId.toString(),
                        "Budget Worker",
                        "Capability score: 7/10",
                        "Good for routine implementation work",
                        "Input cost: 100 micros per million tokens",
                        "Output cost: 200 micros per million tokens");
        assertThat(requests.get(1).systemPrompt()).contains("worker");
        assertThat(requests.get(1).userPrompt()).contains("Design a REST API", plan.content());
        assertThat(requests.get(2).systemPrompt()).contains("supervisor", "Review");
        assertThat(requests.get(2).userPrompt()).contains("Design a REST API", work.content());

        assertThat(result.plan().modelResponse()).isSameAs(plan);
        assertThat(result.plan().executionPlan().summary())
                .isEqualTo("Break the objective into one task");
        assertThat(result.plan().executionPlan().tasks()).singleElement()
                .satisfies(task -> {
                    assertThat(task.instruction()).isEqualTo("Design the REST API");
                    assertThat(task.assignedModelConfigId()).isEqualTo(workerConfigId);
                    assertThat(task.reason()).contains("routine implementation");
                });
        assertThat(result.work()).isSameAs(work);
        assertThat(result.review()).isSameAs(review);
        assertThat(result.totalCostMicros()).isEqualTo(600L);
    }

    @Test
    void rejectsBlankObjectiveBeforeCallingTheModel() {
        AiModelClient modelClient = mock(AiModelClient.class);
        OrchestratorService orchestrator = new OrchestratorService(modelClient, new ObjectMapper());

        assertThatThrownBy(() -> orchestrator.execute("   ", List.of(), modelClient))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("objective must not be blank");
        verifyNoInteractions(modelClient);
    }

    @Test
    void rejectsEmptyModelCatalogBeforeCallingTheSupervisor() {
        AiModelClient modelClient = mock(AiModelClient.class);
        AiModelClient supervisor = mock(AiModelClient.class);
        OrchestratorService orchestrator = new OrchestratorService(modelClient, new ObjectMapper());

        assertThatThrownBy(() -> orchestrator.plan("Design an API", List.of(), supervisor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("model catalog must not be empty");
        verifyNoInteractions(supervisor);
    }

    @Test
    void rejectsInvalidSupervisorPlan() {
        AiModelClient modelClient = mock(AiModelClient.class);
        AiModelClient supervisor = mock(AiModelClient.class);
        when(supervisor.generate(any(AiModelRequest.class)))
                .thenReturn(new AiModelResponse("not-json", 1, 1, 2L));
        OrchestratorService orchestrator = new OrchestratorService(modelClient, new ObjectMapper());
        List<ModelCatalogEntry> modelCatalog = List.of(new ModelCatalogEntry(
                UUID.randomUUID(), "Worker", 5, null, 100L, 200L));

        assertThatThrownBy(() -> orchestrator.plan("Design an API", modelCatalog, supervisor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Supervisor returned an invalid execution plan.");
    }
}
