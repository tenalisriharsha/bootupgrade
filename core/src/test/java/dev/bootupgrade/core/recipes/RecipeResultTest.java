package dev.bootupgrade.core.recipes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeResultTest {

    @Test
    void copiesTheAppliedRecipesListDefensively() {
        List<String> mutable = new java.util.ArrayList<>(List.of("a", "b"));
        RecipeResult result = new RecipeResult(true, 0, mutable, "output");
        mutable.add("c");

        assertThat(result.appliedRecipes()).containsExactly("a", "b");
    }

    @Test
    void rejectsNullFields() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new RecipeResult(true, 0, null, "output"));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new RecipeResult(true, 0, List.of(), null));
    }
}
