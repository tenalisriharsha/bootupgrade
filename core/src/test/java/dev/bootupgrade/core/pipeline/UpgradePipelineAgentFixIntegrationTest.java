package dev.bootupgrade.core.pipeline;

import dev.bootupgrade.core.agent.AgentFixLoop;
import dev.bootupgrade.core.agent.ReadFileTool;
import dev.bootupgrade.core.agent.RunBuildTool;
import dev.bootupgrade.core.agent.WriteFileTool;
import dev.bootupgrade.core.build.DefaultProcessRunner;
import dev.bootupgrade.core.build.MavenBuildRunner;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.git.GitOperations;
import dev.bootupgrade.core.llm.FakeLlmClient;
import dev.bootupgrade.core.llm.LlmResponse;
import dev.bootupgrade.core.llm.ToolCall;
import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepKind;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real end-to-end proof of the Phase 3 agent fallback: copies the
 * {@code legacy-security-service} fixture (whose {@code SecurityConfig}
 * deliberately can't be fully migrated by OpenRewrite — see its javadoc),
 * runs it through the full recipe -> build-check -> agent-fix pipeline with
 * every real collaborator except the LLM (JGit, a real {@code mvn
 * rewrite:run}, a real {@code mvn clean verify}, driven by a {@link
 * FakeLlmClient} scripted with the exact fix a real model would need to find)
 * and asserts the repository ends up on a genuinely green build.
 *
 * <p>After the recipe runs, OpenRewrite migrates most of {@code
 * SecurityConfig} to the {@code SecurityFilterChain} style but leaves its
 * in-memory user builder incomplete ({@code User.builder().username("admin").build()}
 * — no password or role), which throws at context-startup time. The scripted
 * agent reads the file, writes the corrected version, and reruns the build.
 */
class UpgradePipelineAgentFixIntegrationTest {

    private static final String FIXED_SECURITY_CONFIG = """
            package dev.bootupgrade.samples.securityservice;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.security.config.annotation.web.builders.HttpSecurity;
            import org.springframework.security.core.userdetails.User;
            import org.springframework.security.provisioning.InMemoryUserDetailsManager;
            import org.springframework.security.web.SecurityFilterChain;

            import static org.springframework.security.config.Customizer.withDefaults;

            @Configuration
            public class SecurityConfig {

                @Bean
                SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http.authorizeHttpRequests(requests -> requests
                            .requestMatchers("/status/public").permitAll()
                            .anyRequest().authenticated())
                            .httpBasic(withDefaults());
                    return http.build();
                }

                @Bean
                InMemoryUserDetailsManager inMemoryAuthManager() throws Exception {
                    return new InMemoryUserDetailsManager(
                            User.builder().username("admin").password("{noop}password").roles("USER").build());
                }
            }
            """;

    private static final String SECURITY_CONFIG_RELATIVE_PATH =
            "src/main/java/dev/bootupgrade/samples/securityservice/SecurityConfig.java";

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void agentFixesTheIncompleteSecurityMigrationAndTheBuildGoesGreen(@TempDir Path repoPath) throws IOException {
        copyDirectory(findSampleSource(), repoPath);

        UpgradeConfig config = new UpgradeConfig(
                repoPath, 17, "3.0.13", 5, Set.of("read_file", "write_file", "run_build"));

        FakeLlmClient llmClient = FakeLlmClient.scripted(
                LlmResponse.toolCall(new ToolCall("read_file", Map.of("path", SECURITY_CONFIG_RELATIVE_PATH))),
                LlmResponse.toolCall(new ToolCall(
                        "write_file", Map.of("path", SECURITY_CONFIG_RELATIVE_PATH, "content", FIXED_SECURITY_CONFIG))),
                LlmResponse.toolCall(new ToolCall("run_build", Map.of())));

        MavenBuildRunner buildRunner = new MavenBuildRunner(new DefaultProcessRunner(), "mvn", Duration.ofMinutes(3));
        AgentFixLoop agentFixLoop = new AgentFixLoop(llmClient, java.util.List.of(
                new ReadFileTool(), new WriteFileTool(), new RunBuildTool(buildRunner)));

        UpgradePipeline pipeline = new UpgradePipeline(
                new GitOperations(),
                new RecipeRunner(new DefaultProcessRunner(), "mvn", Duration.ofMinutes(3)),
                buildRunner,
                agentFixLoop);

        UpgradeRun run = pipeline.run(config, "legacy-security-service", "8", "2.7.18");

        assertThat(run.steps()).hasSize(3);
        assertThat(run.steps().get(0).kind()).isEqualTo(StepKind.RECIPE);
        assertThat(run.steps().get(0).status())
                .withFailMessage("recipe step failed: %s", run.steps().get(0).diffSummary())
                .isEqualTo(StepStatus.SUCCEEDED);
        assertThat(run.steps().get(1).kind()).isEqualTo(StepKind.BUILD_CHECK);
        assertThat(run.steps().get(1).status())
                .withFailMessage("expected the first build check to still be red after recipes alone")
                .isEqualTo(StepStatus.FAILED);
        assertThat(run.steps().get(2).kind()).isEqualTo(StepKind.AGENT_FIX);
        assertThat(run.steps().get(2).status())
                .withFailMessage("agent fix step failed: %s", run.steps().get(2).diffSummary())
                .isEqualTo(StepStatus.SUCCEEDED);
        assertThat(run.status()).isEqualTo(RunStatus.SUCCEEDED);
        assertThat(llmClient.receivedRequests()).hasSize(3);
    }

    /** Resolves the committed {@code samples/legacy-security-service} fixture relative to this module's working directory. */
    private static Path findSampleSource() {
        Path candidate = Path.of("..", "samples", "legacy-security-service").toAbsolutePath().normalize();
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
