package dev.bootupgrade.core.pipeline;

import dev.bootupgrade.core.build.DefaultProcessRunner;
import dev.bootupgrade.core.build.MavenBuildRunner;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.git.GitOperations;
import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepStatus;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.recipes.RecipeRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real end-to-end proof that the Phase 2 pipeline works: copies the actual
 * {@code legacy-rest-service} sample fixture, then runs the full
 * recipe -> build-check pipeline against it with every real collaborator
 * (JGit, a real {@code mvn rewrite:run}, a real {@code mvn clean verify}) —
 * no fakes anywhere in this test. Needs network access the first time, to
 * resolve the rewrite-maven-plugin and Spring Boot 3 dependencies from
 * Maven Central (both are cached under {@code ~/.m2} afterward).
 */
class UpgradePipelineIntegrationTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void upgradesTheLegacyRestServiceSampleEndToEnd(@TempDir Path repoPath) throws IOException {
        Path sampleSource = findSampleSource();
        copyDirectory(sampleSource, repoPath);

        UpgradeConfig config = new UpgradeConfig(repoPath, 21, "3.0.13", 5, Set.of());
        UpgradePipeline pipeline = new UpgradePipeline(
                new GitOperations(),
                new RecipeRunner(new DefaultProcessRunner(), "mvn", Duration.ofMinutes(3)),
                new MavenBuildRunner(new DefaultProcessRunner(), "mvn", Duration.ofMinutes(3)));

        UpgradeRun run = pipeline.run(config, "legacy-rest-service", "8", "2.7.18");

        assertThat(run.steps()).hasSize(2);
        assertThat(run.steps().get(0).status())
                .withFailMessage("recipe step failed: %s", run.steps().get(0).diffSummary())
                .isEqualTo(StepStatus.SUCCEEDED);
        assertThat(run.steps().get(1).status())
                .withFailMessage("build step failed: %s", run.steps().get(1).diffSummary())
                .isEqualTo(StepStatus.SUCCEEDED);
        assertThat(run.status()).isEqualTo(RunStatus.SUCCEEDED);
    }

    /** Resolves the committed {@code samples/legacy-rest-service} fixture relative to this module's working directory. */
    private static Path findSampleSource() {
        Path candidate = Path.of("..", "samples", "legacy-rest-service").toAbsolutePath().normalize();
        if (!Files.isDirectory(candidate)) {
            throw new IllegalStateException("Expected sample fixture at " + candidate + " but it was not found");
        }
        return candidate;
    }

    private static void copyDirectory(Path source, Path destination) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(path);
                if (isUnderATargetDirectory(relative)) {
                    continue;
                }
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target);
                }
            }
        }
    }

    private static boolean isUnderATargetDirectory(Path relative) {
        for (Path segment : relative) {
            if (segment.toString().equals("target")) {
                return true;
            }
        }
        return false;
    }
}
