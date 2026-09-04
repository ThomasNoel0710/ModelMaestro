package dev.modelmaestro.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.modelmaestro.ai.AiModelClient;
import dev.modelmaestro.ai.AiModelRequest;
import dev.modelmaestro.ai.AiModelResponse;

class OrchestratorServiceTest {

    @Test
    void executesPlanningWorkAndReviewInOrder() {
        AiModelClient modelClient = mock(AiModelClient.class);
        AiModelResponse plan = new AiModelResponse("Break the objective into one task", 10, 20, 100L);
        AiModelResponse work = new AiModelResponse("Completed worker result", 30, 40, 200L);
        AiModelResponse review = new AiModelResponse("Approved final answer", 50, 60, 300L);
        when(modelClient.generate(any(AiModelRequest.class)))
                .thenReturn(plan, work, review);

        OrchestratorService orchestrator = new OrchestratorService(modelClient);

        OrchestrationResult result = orchestrator.execute("  Design a REST API  ");

        ArgumentCaptor<AiModelRequest> requestCaptor = ArgumentCaptor.forClass(AiModelRequest.class);
        verify(modelClient, times(3)).generate(requestCaptor.capture());
        List<AiModelRequest> requests = requestCaptor.getAllValues();

        assertThat(requests.get(0).systemPrompt()).contains("supervisor", "plan");
        assertThat(requests.get(0).userPrompt()).isEqualTo("Design a REST API");
        assertThat(requests.get(1).systemPrompt()).contains("worker");
        assertThat(requests.get(1).userPrompt()).contains("Design a REST API", plan.content());
        assertThat(requests.get(2).systemPrompt()).contains("supervisor", "Review");
        assertThat(requests.get(2).userPrompt()).contains("Design a REST API", work.content());

        assertThat(result.plan()).isSameAs(plan);
        assertThat(result.work()).isSameAs(work);
        assertThat(result.review()).isSameAs(review);
        assertThat(result.totalCostMicros()).isEqualTo(600L);
    }

    @Test
    void rejectsBlankObjectiveBeforeCallingTheModel() {
        AiModelClient modelClient = mock(AiModelClient.class);
        OrchestratorService orchestrator = new OrchestratorService(modelClient);

        assertThatThrownBy(() -> orchestrator.execute("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("objective must not be blank");
        verifyNoInteractions(modelClient);
    }
}
