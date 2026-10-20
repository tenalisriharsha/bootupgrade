package dev.bootupgrade.core.changelog;

import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Renders an {@link UpgradeRun} into human-readable Markdown: a full
 * step-by-step changelog ({@code UPGRADE_LOG.md}) and a short PR description.
 */
public final class ChangelogWriter {

    private ChangelogWriter() {
    }

    /** Converts each step of the run into one {@link ChangelogEntry}, in order. */
    public static List<ChangelogEntry> entriesFor(UpgradeRun run) {
        Objects.requireNonNull(run, "run must not be null");
        return run.steps().stream().map(ChangelogWriter::toEntry).toList();
    }

    private static ChangelogEntry toEntry(UpgradeStep step) {
        String heading = "[%s] %s — %s".formatted(step.status(), step.kind(), step.name());
        String detail = step.diffSummary() == null || step.diffSummary().isBlank()
                ? step.description()
                : step.description() + " (" + step.diffSummary() + ")";
        return new ChangelogEntry(heading, detail);
    }

    /** Renders the full step-by-step changelog for a run as Markdown. */
    public static String renderChangelog(UpgradeRun run) {
        Objects.requireNonNull(run, "run must not be null");
        StringBuilder sb = new StringBuilder();
        sb.append("# Upgrade Log: ").append(run.repositoryName()).append('\n').append('\n');
        sb.append("- Java: ").append(run.sourceJavaVersion()).append(" -> ").append(run.targetJavaVersion()).append('\n');
        sb.append("- Spring Boot: ").append(run.sourceSpringBootVersion()).append(" -> ")
                .append(run.targetSpringBootVersion()).append('\n');
        sb.append("- Branch: ").append(run.branchName()).append('\n');
        sb.append("- Status: ").append(run.status()).append('\n').append('\n');
        sb.append("## Steps").append('\n').append('\n');
        if (run.steps().isEmpty()) {
            sb.append("_No steps recorded yet._").append('\n');
        } else {
            for (ChangelogEntry entry : entriesFor(run)) {
                sb.append(entry.toMarkdownLine()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Renders a short PR description summarizing the run for review. */
    public static String renderPrDescription(UpgradeRun run) {
        Objects.requireNonNull(run, "run must not be null");
        StringBuilder sb = new StringBuilder();
        sb.append("Upgrade ").append(run.repositoryName()).append(": Java ")
                .append(run.sourceJavaVersion()).append(" -> ").append(run.targetJavaVersion())
                .append(", Spring Boot ").append(run.sourceSpringBootVersion()).append(" -> ")
                .append(run.targetSpringBootVersion()).append('\n').append('\n');
        sb.append("Status: ").append(run.status()).append(" (")
                .append(run.succeededStepCount()).append(" succeeded, ")
                .append(run.failedStepCount()).append(" failed, ")
                .append(run.steps().size()).append(" total steps)").append('\n').append('\n');
        if (!run.steps().isEmpty()) {
            sb.append("### Changes").append('\n').append('\n');
            String bullets = entriesFor(run).stream()
                    .map(ChangelogEntry::toMarkdownLine)
                    .collect(Collectors.joining("\n"));
            sb.append(bullets).append('\n');
        }
        return sb.toString();
    }
}
