package dev.modelmaestro.orchestration;

/**
 * Represents the lifecycle state of an orchestration run.
 */
public enum RunStatus {
    CREATED,
    PLANNING,
    RUNNING,
    REVIEWING,
    COMPLETED,
    FAILED,
    CANCELLED,
    BUDGET_EXCEEDED
}
