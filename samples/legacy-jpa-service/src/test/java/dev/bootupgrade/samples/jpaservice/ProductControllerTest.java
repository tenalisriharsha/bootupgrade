package dev.bootupgrade.samples.jpaservice;

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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void postCreatesAProductAndReturns201() throws Exception {
        when(productService.create(anyString(), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(new Product("widget", 9.99));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"widget\",\"price\":9.99}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getAllReturnsEveryProductWhenNoQueryGiven() throws Exception {
        when(productService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/products")).andExpect(status().isOk());
    }

    @Test
    void getAllDelegatesToSearchWhenAQueryIsGiven() throws Exception {
        when(productService.search(eq("bolt"))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/products?q=bolt")).andExpect(status().isOk());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(productService.findById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/1")).andExpect(status().isNotFound());
    }

    @Test
    void deleteByIdReturns204WhenRemoved() throws Exception {
        when(productService.deleteById(anyLong())).thenReturn(true);

        mockMvc.perform(delete("/products/1")).andExpect(status().isNoContent());
    }
}
