package dev.modelmaestro.orchestration.plan;

import java.util.List;

public record ExecutionPlan(
        String summary,
        List<PlannedTask> tasks) {

    public ExecutionPlan {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Plan summary must not be blank.");
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Execution plan must contain at least one task.");
        }
        summary = summary.strip();
        tasks = List.copyOf(tasks);
    }
}
