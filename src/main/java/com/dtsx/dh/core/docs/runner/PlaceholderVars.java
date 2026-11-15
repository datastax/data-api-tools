package com.dtsx.dh.core.docs.runner;

import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.common.CliException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = { @JsonCreator })
public class PlaceholderVars {
    public static PlaceholderVars EMPTY = new PlaceholderVars();

    public record PlaceholderVar(String regex, String value) {}

    private Map<String, PlaceholderVar> vars = new HashMap<>();

    public Map<String, PlaceholderVar> getAll(ClientLanguage language) {
        val result = new HashMap<String, PlaceholderVar>();

        vars.forEach((key, var) -> {
            val newVar = new PlaceholderVar(var.regex, resolveVariables(this, language, null, var.value()));
            result.put(key, newVar);
        });

        return result;
    }

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{.+?}");

    public static String resolveVariables(PlaceholderVars vars, ClientLanguage language, @Nullable BaseFixturePool.FixtureIndex index, String input) {
        return resolveVariables(vars, language, index, input, new HashSet<>());
    }

    private static String resolveVariables(PlaceholderVars vars, ClientLanguage language, @Nullable BaseFixturePool.FixtureIndex index, String input, Set<String> visitedVars) {
        val matcher = VARIABLE_PATTERN.matcher(input);
        val sb = new StringBuilder();

        while (matcher.find()) {
            val replacement = resolveSingleVariable(vars, language, index, matcher.group(), visitedVars);
            matcher.appendReplacement(sb, replacement);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private static @NotNull String resolveSingleVariable(PlaceholderVars vars, ClientLanguage language, @Nullable BaseFixturePool.FixtureIndex index, String variable, Set<String> visitedVars) {
        if (visitedVars.contains(variable)) {
            throw new CliException("Circular dependency detected in variable resolution: " + variable + " (resolution chain: " + String.join(" -> ", visitedVars) + " -> " + variable + ")");
        }

        val varName = variable.substring(2, variable.length() - 1); // strip ${ and }

        return switch (varName) {
            case "LANGUAGE" -> language.name().toLowerCase();
            case "NAME_ROOT" -> {
                if (index == null) {
                    throw new CliException("${NAME_ROOT} used in an invalid context. It should only be used under in snapshot source blocks for the time being.");
                }
                yield index.toNameRoot();
            }
            default -> {
                val userVar = vars.vars.get(varName);

                if (userVar != null) {
                    val newVisitedVars = new HashSet<>(visitedVars);
                    newVisitedVars.add(variable);
                    yield resolveVariables(vars, language, index, userVar.value(), newVisitedVars);
                } else {
                    throw new CliException("Unrecognized variable in placeholder key '" + variable + "'. Expected one of ${LANGUAGE}, ${NAME_ROOT}, or a user-defined variable.");
                }
            }
        };
    }
}
