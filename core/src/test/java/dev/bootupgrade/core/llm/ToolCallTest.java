package dev.bootupgrade.core.llm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCallTest {

    @Test
    void defaultsArgumentsToEmptyMapWhenNull() {
        ToolCall call = new ToolCall("run_build", null);

        assertThat(call.arguments()).isEmpty();
    }

    @Test
    void argumentsAreImmutable() {
        ToolCall call = new ToolCall("run_build", Map.of("goal", "verify"));

        assertThatThrownBy(() -> call.arguments().put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankToolName() {
        assertThatThrownBy(() -> new ToolCall("  ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullToolName() {
        assertThatThrownBy(() -> new ToolCall(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
