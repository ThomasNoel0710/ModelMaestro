package dev.modelmaestro.orchestration;

import java.util.UUID;

public class RunNotFoundException extends RuntimeException {

    private final UUID runId;

    public RunNotFoundException(UUID runId) {
        super("Run " + runId + " was not found.");
        this.runId = runId;
    }

    public UUID getRunId() {
        return runId;
    }
}

