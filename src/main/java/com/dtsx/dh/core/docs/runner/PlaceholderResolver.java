package com.dtsx.dh.core.docs.runner;

import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.common.ClientLanguage;
import lombok.val;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Map<String, TriFunction<BaseScriptRunnerCtx, Placeholders, Optional<ClientLanguage>, Optional<String>>> DYNAMIC_PLACEHOLDERS = Map.of(
        "APPLICATION_TOKEN", (ctx, _, _) -> Optional.of(ctx.connectionInfo().token()),
        "API_ENDPOINT", (ctx, _, _) -> Optional.of(ctx.connectionInfo().endpoint()),
        "USERNAME", (ctx, _, _) -> ctx.connectionInfo().username(),
        "PASSWORD", (ctx, _, _) -> ctx.connectionInfo().password(),
        "KEYSPACE_NAME", (_, phs, _) -> Optional.of(phs.keyspaceName()),
        "TABLE_NAME", (_, phs, _) -> phs.tableName(),
        "COLLECTION_NAME", (_, phs, _) -> phs.collectionName()
    );

    private static final Map<String, String> STATIC_PLACEHOLDERS = Map.of(
        "DATABASE_NAME", "whatever_db_name",
        "DATABASE_ID", "whatever_db_id",
        "OLD_COLLECTION_NAME", "whatever_old_name",
        "NEW_COLLECTION_NAME", "whatever_new_name"
    );

    private static final Pattern PLACEHOLDER = Pattern.compile("\\*\\*(\\w+)\\*\\*");

    private static final Map<String, String> VECTORS = Map.of(
        "0.08, -0.62, 0.39", randomVectorArray("vec1", false),
        "0.08f, -0.62f, 0.39f", randomVectorArray("vec1", true),
        "0.12, 0.53, 0.32", randomVectorArray("vec2", false),
        "0.12f, 0.53f, 0.32f", randomVectorArray("vec2", true),
        "PaPXCr8euFI+x64U", randomVectorBinary("bin1"),
        "PfXCjz8HrhQ+o9cK", randomVectorBinary("bin2")
    );

    public static String replacePlaceholders(BaseScriptRunnerCtx ctx, Placeholders placeholders, ClientLanguage lang, String src) {
        for (val var : placeholders.vars().getAll(lang).values()) {
            src = src.replaceAll(var.regex(), var.value());
        }

        val m = PLACEHOLDER.matcher(src);
        val out = new StringBuilder(src.length());

        while (m.find()) {
            val key = m.group(1);

            if (DYNAMIC_PLACEHOLDERS.containsKey(key)) {
                val value = DYNAMIC_PLACEHOLDERS.get(key).apply(ctx, placeholders, Optional.of(lang)).orElseThrow(() -> new RunException("Missing value for placeholder: **" + key + "**"));
                m.appendReplacement(out, Matcher.quoteReplacement(value));
            } else if (STATIC_PLACEHOLDERS.containsKey(key)) {
                m.appendReplacement(out, Matcher.quoteReplacement(STATIC_PLACEHOLDERS.get(key)));
            } else {
                throw new RunException("Unknown placeholder: **" + key + "**");
            }
        }

        m.appendTail(out);

        VECTORS.forEach((vector, replacement) -> {
            val index = out.indexOf(vector);

            if (index >= 0) {
                out.replace(index, index + vector.length(), replacement);
            }
        });

        return out.toString();
    }

    public static HashMap<String, String> mkEnvVars(BaseScriptRunnerCtx ctx, Placeholders placeholders, Optional<ClientLanguage> maybeLang) {
        val envVars = new HashMap<String, String>();

        DYNAMIC_PLACEHOLDERS.forEach((key, func) -> {
            func.apply(ctx, placeholders, maybeLang).ifPresent(v -> envVars.put(key, v));
        });

        envVars.putAll(STATIC_PLACEHOLDERS);

        maybeLang.ifPresent((lang) -> {
            placeholders.vars().getAll(lang).forEach((key, var) -> {
                envVars.put(key, var.value());
            });
        });

        return envVars;
    }

    private static String randomVectorArray(String seed, boolean appendF) {
        val random = new Random(seed.hashCode());
        val sb = new StringBuilder();

        for (int i = 0; i < 1024; i++) {
            sb.append('0');
            sb.append('.');
            sb.append(random.nextInt(1, 10));
            if (appendF) {
                sb.append('f');
            }
            if (i < 1023) {
                sb.append(',');
            }
        }

        return sb.toString();
    }

    private static String randomVectorBinary(String seed) {
        val random = new Random(seed.hashCode());
        val bytes = new byte[1024 * Float.BYTES];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) random.nextInt(40, 60); // random bounds that don't seem to cause issues
        }

        return Base64.getEncoder().encodeToString(bytes);
    }
}
