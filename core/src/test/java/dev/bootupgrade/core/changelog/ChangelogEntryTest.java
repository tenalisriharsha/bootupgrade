package dev.bootupgrade.core.changelog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangelogEntryTest {

    @Test
    void toMarkdownLineJoinsHeadingAndDetailWithDash() {
        ChangelogEntry entry = new ChangelogEntry("[SUCCEEDED] RECIPE", "migrated javax to jakarta");

        assertThat(entry.toMarkdownLine()).isEqualTo("- [SUCCEEDED] RECIPE — migrated javax to jakarta");
    }

    @Test
    void toMarkdownLineOmitsDashSeparatorWhenDetailBlank() {
        ChangelogEntry entry = new ChangelogEntry("[SUCCEEDED] RECIPE", "");

        assertThat(entry.toMarkdownLine()).isEqualTo("- [SUCCEEDED] RECIPE");
    }

    @Test
    void rejectsBlankHeading() {
        assertThatThrownBy(() -> new ChangelogEntry("  ", "detail"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullFields() {
        assertThatThrownBy(() -> new ChangelogEntry(null, "detail")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ChangelogEntry("heading", null)).isInstanceOf(NullPointerException.class);
    }
}
