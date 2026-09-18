package dev.modelmaestro.ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ModelConfigApiTest {
    @Autowired private WebApplicationContext context;
    @Autowired private ModelConfigRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createsListsDisablesAndReenablesModel() throws Exception {
        String name = "API test " + UUID.randomUUID();
        try {
            mvc.perform(post("/api/v1/models").contentType(MediaType.APPLICATION_JSON)
                    .content(modelJson(name, 7)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.enabled").value(true));
            UUID id = repository.findAll().stream()
                    .filter(m -> m.getDisplayName().equals(name)).findFirst().orElseThrow().getId();
            mvc.perform(patch("/api/v1/models/{id}/enabled", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
            assertThat(repository.findById(id).orElseThrow().isEnabled()).isFalse();
            assertThat(repository.findAllByEnabledTrueOrderByCapabilityScoreDesc())
                    .extracting(ModelConfig::getId).doesNotContain(id);
            mvc.perform(get("/api/v1/models")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == '" + id + "')].enabled")
                            .value(org.hamcrest.Matchers.contains(false)));
            mvc.perform(patch("/api/v1/models/{id}/enabled", id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
            assertThat(repository.findById(id).orElseThrow().isEnabled()).isTrue();
        } finally {
            repository.findAll().stream().filter(m -> m.getDisplayName().equals(name))
                    .forEach(repository::delete);
        }
    }

    @Test
    void rejectsMissingEnabledAndUnknownModel() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(patch("/api/v1/models/{id}/enabled", id)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/models/{id}/enabled", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidCreationParameters() throws Exception {
        mvc.perform(post("/api/v1/models").contentType(MediaType.APPLICATION_JSON)
                .content(modelJson("Invalid score", 11))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/models").contentType(MediaType.APPLICATION_JSON)
                .content(modelJson("", 7))).andExpect(status().isBadRequest());
    }

    private String modelJson(String name, int score) {
        return """
                {"displayName":"%s","protocol":"OPENAI_COMPATIBLE",
                 "baseUrl":"https://example.test/v1","modelId":"test-model",
                 "capabilityScore":%d,"inputCostMicrosPerMillionTokens":100,
                 "outputCostMicrosPerMillionTokens":200,"enabled":true}
                """.formatted(name, score);
    }
}
