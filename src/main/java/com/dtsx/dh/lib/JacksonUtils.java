package com.dtsx.dh.lib;

import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.common.CliException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class JacksonUtils {
    private static final YAMLMapper YAML = YAMLMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .build();

    @SneakyThrows
    public static <T> T parseYaml(Path file, Class<T> clazz) {
        try (val reader = Files.newBufferedReader(file)) {
            return YAML.readValue(reader, clazz);
        }
    }

    private static final JsonMapper JSON;

    static {
        val pp = new DefaultPrettyPrinter()
            .withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        JSON = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .defaultPrettyPrinter(pp)
            .build();
    }

    @SneakyThrows
    public static <T> T parseJson(String string, Class<T> clazz) {
        try {
            return JSON.readValue(string, clazz);
        } catch (Exception e) {
            throw new CliException("Failed to parse JSON into " + clazz.getSimpleName() + ": " + e.getMessage() + "\nJSON:\n" + string, e);
        }
    }

    public static <T> List<T> parseJsonLines(String string, Class<T> clazz) {
        val res = new ArrayList<T>();

        try (val parser = JSON.createParser(string)) {
            while (!parser.isClosed()) {
                val token = parser.nextToken();
                if (token == null) {
                    break;
                }
                res.add(JSON.readValue(parser, clazz));
            }
        } catch (Exception e) {
            throw new CliException("Failed to parse JSON lines into " + clazz.getSimpleName() + ": " + e.getMessage() + "\nJSON:\n" + string, e);
        }

        return res;
    }

    @SneakyThrows
    public static <T> T convertValue(Object fromValue, TypeReference<T> ref) {
        try {
            return JSON.convertValue(fromValue, ref);
        } catch (Exception e) {
            throw new CliException("Failed to convert value to " + ref.getType().getTypeName() + ": " + e.getMessage() + "\nValue:\n" + formatJsonCompact(fromValue), e);
        }
    }

    @SneakyThrows
    public static <T> T convertValue(Object fromValue, Class<T> clazz) {
        try {
            return JSON.convertValue(fromValue, clazz);
        } catch (Exception e) {
            throw new CliException("Failed to convert value to " + clazz.getSimpleName() + ": " + e.getMessage() + "\nValue:\n" + formatJsonCompact(fromValue), e);
        }
    }

    @SneakyThrows
    public static String formatJsonCompact(Object obj) {
        return JSON.writeValueAsString(obj);
    }

    @SneakyThrows
    public static String formatJsonPretty(Object obj) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    // TODO probably move this somewhere else
    public static String runJq(BaseScriptRunnerCtx ctx, String json, String filter) {
        val args = new String[] { "-n", "--argjson", "input", json, "$input | " + filter };

        val res = ExternalPrograms.jq(ctx).run(args);

        if (res.exitCode() != 0) {
            throw new CliException("Failed to run jq with args " + Arrays.toString(args) + "\n: " + res.output());
        }

        return res.output();
    }
}
