package dev.bootupgrade.core.recipes;

import java.util.List;
import java.util.Objects;

/** The outcome of applying a set of OpenRewrite recipes to a target repository. */
public record RecipeResult(boolean success, int exitCode, List<String> appliedRecipes, String output) {

    public RecipeResult {
        Objects.requireNonNull(appliedRecipes, "appliedRecipes must not be null");
        Objects.requireNonNull(output, "output must not be null");
        appliedRecipes = List.copyOf(appliedRecipes);
    }
}
