package dev.modelmaestro.ai.config;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ModelConfigNotFoundException extends RuntimeException {
    public ModelConfigNotFoundException(UUID id) {
        super("Model configuration %s was not found.".formatted(id));
    }
}
