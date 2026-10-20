package dev.bootupgrade.samples.securityservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StatusController.class)
@Import(SecurityConfig.class)
class StatusControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeartbeatJob heartbeatJob;

    @Test
    void publicStatusIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/status/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void privateStatusIsRejectedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/status/private")).andExpect(status().isUnauthorized());
    }

    @Test
    void privateStatusIsAccessibleWithValidCredentials() throws Exception {
        mockMvc.perform(get("/status/private").with(httpBasic("admin", "password")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser
    void heartbeatCountReflectsTheJob() throws Exception {
        when(heartbeatJob.beatCount()).thenReturn(5L);

        mockMvc.perform(get("/status/heartbeat-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}
