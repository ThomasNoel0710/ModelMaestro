package dev.modelmaestro.orchestration;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SupervisorUnavailableException extends RuntimeException {
    public SupervisorUnavailableException(UUID id) {
        super("Supervisor configuration %s is disabled.".formatted(id));
    }
}
