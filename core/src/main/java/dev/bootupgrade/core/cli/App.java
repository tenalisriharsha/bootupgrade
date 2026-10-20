package dev.bootupgrade.core.cli;

import dev.bootupgrade.core.config.UpgradeConfig;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Minimal CLI entry point. Phase 1 only proves the module is wired end to
 * end; the actual recipe/build/agent pipeline is wired in from Phase 2 on.
 */
public final class App {

    public static final String VERSION = "0.1.0-SNAPSHOT";

    private App() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Runs the CLI against {@code args}, writing output to {@code out}. Returns a process exit code. */
    public static int run(String[] args, PrintStream out) {
        out.println("bootupgrade " + VERSION + " — agentic Spring Boot upgrade tool");
        if (args.length == 0) {
            out.println("Usage: bootupgrade <path-to-repository>");
            return 1;
        }
        Path repositoryPath = Path.of(args[0]);
        UpgradeConfig config = UpgradeConfig.defaultFor(repositoryPath);
        out.println("Repository: " + config.repositoryPath());
        out.println("Target Java version: " + config.targetJavaVersion());
        out.println("Target Spring Boot version: " + config.targetSpringBootVersion());
        out.println("Max agent iterations: " + config.maxAgentIterations());
        return 0;
    }
}
