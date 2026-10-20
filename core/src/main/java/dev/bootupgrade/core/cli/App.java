package dev.bootupgrade.core.cli;

import dev.bootupgrade.core.build.DefaultProcessRunner;
import dev.bootupgrade.core.build.MavenBuildRunner;
import dev.bootupgrade.core.changelog.ChangelogWriter;
import dev.bootupgrade.core.config.UpgradeConfig;
import dev.bootupgrade.core.git.GitOperations;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;
import dev.bootupgrade.core.pipeline.UpgradePipeline;
import dev.bootupgrade.core.recipes.RecipeRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Command-line entry point: upgrades the Maven project at the given path by running the
 * OpenRewrite recipes on a new git branch and then checking the build.
 *
 * <p>Exit codes: 0 succeeded, 1 bad usage or path, 2 partial (build still red), 3 failed.
 */
public final class App {

    public static final String VERSION = "0.1.0-SNAPSHOT";

    /** Runs one upgrade; a seam so tests do not need Maven or git. */
    @FunctionalInterface
    public interface UpgradeExecutor {
        UpgradeRun execute(UpgradeConfig config, String repositoryName, String sourceJava, String sourceSpringBoot);
    }

    private App() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream out) {
        return run(args, out, App::realUpgrade);
    }

    public static int run(String[] args, PrintStream out, UpgradeExecutor executor) {
        out.println("bootupgrade " + VERSION + " - agentic Spring Boot upgrade tool");
        String repository = null;
        String reportTo = null;
        String outDir = ".";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--report-to" -> {
                    if (i + 1 >= args.length) {
                        return usage(out, "--report-to needs a URL");
                    }
                    reportTo = args[++i];
                }
                case "--out" -> {
                    if (i + 1 >= args.length) {
                        return usage(out, "--out needs a directory");
                    }
                    outDir = args[++i];
                }
                default -> {
                    if (args[i].startsWith("--") || repository != null) {
                        return usage(out, "unexpected argument: " + args[i]);
                    }
                    repository = args[i];
                }
            }
        }
        if (repository == null) {
            return usage(out, null);
        }
        Path repositoryPath = Path.of(repository).toAbsolutePath().normalize();
        if (!Files.isDirectory(repositoryPath) || !Files.isRegularFile(repositoryPath.resolve("pom.xml"))) {
            out.println("error: " + repositoryPath + " is not a Maven project (no pom.xml found)");
            return 1;
        }

        UpgradeConfig config = UpgradeConfig.defaultFor(repositoryPath);
        SourceVersions source = SourceVersions.detect(repositoryPath);
        String name = repositoryPath.getFileName().toString();
        out.println("Repository:  " + repositoryPath);
        out.println("Upgrading:   Java " + source.java() + " -> " + config.targetJavaVersion()
                + ", Spring Boot " + source.springBoot() + " -> " + config.targetSpringBootVersion());
        out.println();

        UpgradeRun result = executor.execute(config, name, source.java(), source.springBoot());

        for (UpgradeStep step : result.steps()) {
            out.println(String.format("  [%-9s] %-26s %s", step.status(), step.name(), firstLine(step.diffSummary())));
        }
        out.println();
        out.println("Result: " + result.status() + "  (branch " + result.branchName() + ")");

        try {
            Path dir = Path.of(outDir);
            Files.createDirectories(dir);
            Path changelog = dir.resolve("bootupgrade-" + name + "-changelog.md");
            Path prDescription = dir.resolve("bootupgrade-" + name + "-pr.md");
            Files.writeString(changelog, ChangelogWriter.renderChangelog(result));
            Files.writeString(prDescription, ChangelogWriter.renderPrDescription(result));
            out.println("Changelog:   " + changelog);
            out.println("PR text:     " + prDescription);
        } catch (IOException e) {
            out.println("warning: could not write the changelog: " + e.getMessage());
        }

        if (reportTo != null) {
            try {
                int status = new RunReporter().report(reportTo, result);
                out.println("Reported to " + reportTo + " (HTTP " + status + ")");
            } catch (IOException | InterruptedException | RuntimeException e) {
                out.println("warning: could not report to " + reportTo + ": " + e.getMessage());
            }
        }
        return switch (result.status()) {
            case SUCCEEDED -> 0;
            case PARTIAL -> 2;
            default -> 3;
        };
    }

    private static UpgradeRun realUpgrade(UpgradeConfig config, String name, String sourceJava, String sourceBoot) {
        DefaultProcessRunner processes = new DefaultProcessRunner();
        UpgradePipeline pipeline = new UpgradePipeline(
                new GitOperations(),
                new RecipeRunner(processes, "mvn", Duration.ofMinutes(10)),
                new MavenBuildRunner(processes, "mvn", Duration.ofMinutes(10)));
        return pipeline.run(config, name, sourceJava, sourceBoot);
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        String line = text.lines().findFirst().orElse("");
        return line.length() > 110 ? line.substring(0, 107) + "..." : line;
    }

    private static int usage(PrintStream out, String problem) {
        if (problem != null) {
            out.println("error: " + problem);
        }
        out.println("Usage: bootupgrade <path-to-maven-project> [--out <dir>] [--report-to <api-url>]");
        out.println();
        out.println("  Creates an upgrade branch in the project, applies the OpenRewrite recipes, commits,");
        out.println("  and runs 'mvn clean verify'. Writes a changelog and PR description to --out (default .).");
        out.println("  --report-to  POST the finished run to a bootupgrade API (for the dashboard).");
        out.println("Exit codes: 0 succeeded, 1 bad usage, 2 partial (build still red), 3 failed.");
        return 1;
    }
}
