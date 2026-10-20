package dev.bootupgrade.core.changelog;

import java.util.Objects;

/** One rendered line item of a changelog, derived from a single upgrade step. */
public record ChangelogEntry(String heading, String detail) {

    public ChangelogEntry {
        Objects.requireNonNull(heading, "heading must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        if (heading.isBlank()) {
            throw new IllegalArgumentException("heading must not be blank");
        }
    }

    /** Renders as a single Markdown list item. */
    public String toMarkdownLine() {
        return detail.isBlank() ? "- " + heading : "- " + heading + " — " + detail;
    }
}
