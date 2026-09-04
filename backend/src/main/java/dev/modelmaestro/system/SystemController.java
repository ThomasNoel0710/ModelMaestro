package dev.modelmaestro.system;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping
    public Map<String, Object> getSystemInfo() {
        return Map.of(
                "name", "ModelMaestro",
                "status", "ready",
                "timestamp", Instant.now());
    }
}

