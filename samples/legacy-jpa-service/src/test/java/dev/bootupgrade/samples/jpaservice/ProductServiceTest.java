package dev.bootupgrade.samples.jpaservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createSavesAProductBuiltFromTheGivenNameAndPrice() {
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create("widget", 4.5);

        assertThat(created.getName()).isEqualTo("widget");
        assertThat(created.getPrice()).isEqualTo(4.5);
    }

    @Test
    void deleteByIdReturnsFalseWhenTheProductDoesNotExist() {
        when(productRepository.existsById(anyLong())).thenReturn(false);

        assertThat(productService.deleteById(42L)).isFalse();
    }

    @Test
    void deleteByIdReturnsTrueAndDelegatesWhenTheProductExists() {
        when(productRepository.existsById(anyLong())).thenReturn(true);

        assertThat(productService.deleteById(42L)).isTrue();
    }

    @Test
    void findByIdDelegatesToTheRepository() {
        Product product = new Product("widget", 1.0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.findById(1L)).contains(product);
    }

    @Test
    void searchDelegatesToTheRepository() {
        when(productRepository.findByNameContainingIgnoreCase("bolt")).thenReturn(Collections.emptyList());

        assertThat(productService.search("bolt")).isEmpty();
    }
}
