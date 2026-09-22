package com.dtsx.docs.core.planner.meta;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import tools.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PerLanguageToggle<E> {
    protected final Map<ClientLanguage, E> languages;

    public static <T extends PerLanguageToggle<E>, E> T parse(Function<Map<ClientLanguage, E>, T> cons, TestCtx ctx, Optional<Object> maybeRaw, TypeReference<Map<ClientLanguage, E>> elemsType) {
        if (maybeRaw.isEmpty()) {
            return cons.apply(Map.of());
        }

        val raw = maybeRaw.get();

        try {
            val converted = JacksonUtils.convertValue(Map.of(ClientLanguage.JAVA, raw), elemsType); // disgusting hack to reuse the same TypeReference for both Map<ClientLanguage, E> and E

            return cons.apply(
                ctx.languages().stream().collect(toMap(lang -> lang, _ -> converted.get(ClientLanguage.JAVA)))
            );
        } catch (Exception _) {}

        try {
            return cons.apply(
                JacksonUtils.convertValue(raw, elemsType)
            );
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid share config: " + raw, e);
        }
    }
}
