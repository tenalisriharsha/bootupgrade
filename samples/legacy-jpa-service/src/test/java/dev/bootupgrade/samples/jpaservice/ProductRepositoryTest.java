package dev.bootupgrade.samples.jpaservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void savesAndReloadsAProduct() {
        Product saved = productRepository.save(new Product("widget", 9.99));

        Optional<Product> found = productRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("widget");
        assertThat(found.get().getPrice()).isEqualTo(9.99);
    }

    @Test
    void findsProductsByCaseInsensitiveNameFragment() {
        productRepository.save(new Product("Steel Bolt", 1.5));
        productRepository.save(new Product("Copper Wire", 2.5));

        List<Product> results = productRepository.findByNameContainingIgnoreCase("bolt");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Steel Bolt");
    }

    @Test
    void deleteByIdRemovesTheProduct() {
        Product saved = productRepository.save(new Product("gone soon", 3.0));

        productRepository.deleteById(saved.getId());

        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }
}
