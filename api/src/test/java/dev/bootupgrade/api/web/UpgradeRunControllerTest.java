package dev.bootupgrade.api.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bootupgrade.api.dto.CreateUpgradeRunRequest;
import dev.bootupgrade.api.dto.CreateUpgradeStepRequest;
import dev.bootupgrade.api.model.RunStatus;
import dev.bootupgrade.api.model.StepKind;
import dev.bootupgrade.api.model.StepStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UpgradeRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createListAndFetchARunThroughTheApi() throws Exception {
        var request = new CreateUpgradeRunRequest(
                "legacy-security-service", "8", "21", "2.7.18", "3.3.5", "bootupgrade/run-security",
                RunStatus.PARTIAL,
                List.of(new CreateUpgradeStepRequest(
                        "Agent fix", StepKind.AGENT_FIX, StepStatus.SUCCEEDED,
                        "Completed in-memory user builder", "1 file changed", Instant.now(), Instant.now())));

        var createResult = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repositoryName").value("legacy-security-service"))
                .andExpect(jsonPath("$.steps", hasSize(1)))
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/runs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchName").value("bootupgrade/run-security"));
    }

    @Test
    void returnsNotFoundForUnknownRun() throws Exception {
        mockMvc.perform(get("/api/runs/{id}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        String invalidJson = """
                {"repositoryName": "", "sourceJavaVersion": "8"}
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void successRateReflectsCreatedRuns() throws Exception {
        mockMvc.perform(post("/api/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateUpgradeRunRequest(
                        "legacy-jpa-service", "8", "21", "2.7.18", "3.3.5", "bootupgrade/run-jpa",
                        RunStatus.SUCCEEDED, List.of()))));

        mockMvc.perform(get("/api/runs/stats/success-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRuns").value(1))
                .andExpect(jsonPath("$.succeededRuns").value(1))
                .andExpect(jsonPath("$.byRepository[0].repositoryName").value("legacy-jpa-service"));
    }
}
