package dev.bootupgrade.core.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeConfigTest {

    @Test
    void defaultForProducesSensibleDefaults() {
        UpgradeConfig config = UpgradeConfig.defaultFor(Path.of("/tmp/repo"));

        assertThat(config.targetJavaVersion()).isEqualTo(21);
        assertThat(config.targetSpringBootVersion()).isEqualTo("3.3.5");
        assertThat(config.maxAgentIterations()).isEqualTo(5);
        assertThat(config.allowedToolNames()).containsExactlyInAnyOrder("read_file", "write_file", "run_build");
    }

    @Test
    void rejectsJavaVersionBelowMinimumSupported() {
        assertThatThrownBy(() -> new UpgradeConfig(Path.of("/tmp/repo"), 8, "3.3.5", 5, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetJavaVersion");
    }

    @Test
    void rejectsBlankSpringBootVersion() {
        assertThatThrownBy(() -> new UpgradeConfig(Path.of("/tmp/repo"), 21, "  ", 5, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetSpringBootVersion");
    }

    @Test
    void rejectsNegativeMaxAgentIterations() {
        assertThatThrownBy(() -> new UpgradeConfig(Path.of("/tmp/repo"), 21, "3.3.5", -1, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxAgentIterations");
    }

    @Test
    void allowsZeroMaxAgentIterationsMeaningRecipesOnly() {
        UpgradeConfig config = new UpgradeConfig(Path.of("/tmp/repo"), 21, "3.3.5", 0, Set.of());

        assertThat(config.maxAgentIterations()).isZero();
    }

    @Test
    void rejectsNullRepositoryPath() {
        assertThatThrownBy(() -> new UpgradeConfig(null, 21, "3.3.5", 5, Set.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allowedToolNamesIsImmutable() {
        UpgradeConfig config = new UpgradeConfig(Path.of("/tmp/repo"), 21, "3.3.5", 5, Set.of("read_file"));

        assertThatThrownBy(() -> config.allowedToolNames().add("write_file"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
