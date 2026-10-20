package dev.bootupgrade.core.recipes;

import dev.bootupgrade.core.build.ProcessResult;
import dev.bootupgrade.core.build.ProcessRunner;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs deterministic OpenRewrite recipes against a target repository via the
 * {@code rewrite-maven-plugin}, before any LLM involvement. The target
 * repository's {@code pom.xml} must already declare the plugin (with the
 * recipe artifact, e.g. {@code rewrite-spring}, as a plugin dependency) —
 * this runner only selects which of its recipes run and executes it.
 *
 * <p>Mechanical, free, and (unlike the LLM agent fallback in Phase 3) fully
 * deterministic: the same repository and recipe list always produce the
 * same change.
 */
public final class RecipeRunner {

    /** Single composite recipe covering the javax->jakarta rename plus Spring Boot 2->3 config/property migration. */
    public static final List<String> DEFAULT_RECIPES =
            List.of("org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0");

    private final ProcessRunner processRunner;
    private final String mvnExecutable;
    private final Duration timeout;

    public RecipeRunner(ProcessRunner processRunner, String mvnExecutable, Duration timeout) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner must not be null");
        this.mvnExecutable = Objects.requireNonNull(mvnExecutable, "mvnExecutable must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    public RecipeRunner(ProcessRunner processRunner) {
        this(processRunner, "mvn", Duration.ofMinutes(10));
    }

    /** Applies {@link #DEFAULT_RECIPES} to {@code repoPath} in place. */
    public RecipeResult run(Path repoPath) {
        return run(repoPath, DEFAULT_RECIPES);
    }

    /** Applies {@code activeRecipes} to {@code repoPath} in place via {@code mvn rewrite:run}. */
    public RecipeResult run(Path repoPath, List<String> activeRecipes) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        Objects.requireNonNull(activeRecipes, "activeRecipes must not be null");
        if (activeRecipes.isEmpty()) {
            throw new IllegalArgumentException("activeRecipes must not be empty");
        }

        List<String> command = new ArrayList<>();
        command.add(mvnExecutable);
        command.add("-B");
        command.add("-f");
        command.add(repoPath.toString());
        command.add("rewrite:run");
        command.add("-Drewrite.activeRecipes=" + String.join(",", activeRecipes));

        ProcessResult result = processRunner.run(command, repoPath, timeout);
        return new RecipeResult(result.succeeded(), result.exitCode(), activeRecipes,
                result.stdout() + result.stderr());
    }
}
