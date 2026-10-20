package dev.bootupgrade.samples.restservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(WidgetController.class)
class WidgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WidgetService widgetService;

    @Test
    void postCreatesAWidgetAndReturns201() throws Exception {
        when(widgetService.create(any(WidgetRequest.class))).thenReturn(new Widget(1L, "bolt", 10));

        mockMvc.perform(post("/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bolt\",\"quantity\":10}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"id\":1,\"name\":\"bolt\",\"quantity\":10}"));
    }

    @Test
    void postRejectsABlankNameWith400() throws Exception {
        mockMvc.perform(post("/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"quantity\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(widgetService.findById(eq(42L))).thenReturn(Optional.empty());

        mockMvc.perform(get("/widgets/42")).andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsTheWidgetWhenPresent() throws Exception {
        when(widgetService.findById(eq(1L))).thenReturn(Optional.of(new Widget(1L, "bolt", 10)));

        mockMvc.perform(get("/widgets/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"name\":\"bolt\",\"quantity\":10}"));
    }

    @Test
    void getAllReturnsEveryWidget() throws Exception {
        when(widgetService.findAll()).thenReturn(Collections.singletonList(new Widget(1L, "bolt", 10)));

        mockMvc.perform(get("/widgets")).andExpect(status().isOk());
    }

    @Test
    void deleteByIdReturns204WhenRemoved() throws Exception {
        when(widgetService.deleteById(eq(1L))).thenReturn(true);

        mockMvc.perform(delete("/widgets/1")).andExpect(status().isNoContent());
    }

    @Test
    void deleteByIdReturns404WhenNotFound() throws Exception {
        when(widgetService.deleteById(eq(1L))).thenReturn(false);

        mockMvc.perform(delete("/widgets/1")).andExpect(status().isNotFound());
    }
}
