package com.dtsx.dh.core.docs.runner.drivers.impls;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;
import tools.jackson.databind.node.ArrayNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class PythonDriver extends ClientDriver {
    public PythonDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.PYTHON;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::python);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val python = ExternalPrograms.python(ctx);

        val mkVenv = python.run(execEnv.envDir(), "-m", "venv", ".venv");
        if (mkVenv.notOk()) {
            throw new RuntimeException("Failed to create Python virtual environment:\n" + mkVenv.output());
        }

        replaceArtifactPlaceholder(execEnv, "requirements.txt");

        val install = ExternalPrograms.custom().run(execEnv.envDir(), ".venv/bin/pip", "install", "-U", "-r", "requirements.txt", artifact());
        if (install.notOk()) {
            throw new RuntimeException("Failed to install Python dependencies:\n" + install.output());
        }

        return execEnv.envDir().resolve("example.py");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ctx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = """
            import json as _json_module
            from astrapy.data.utils.table_converters import preprocess_table_payload, FullSerdesOptions
            
            _serdes_options = FullSerdesOptions(
                binary_encode_vectors=False,
                custom_datatypes_in_reading=True,
                unroll_iterables_to_lists=True,
                use_decimals_in_collections=False,
                encode_maps_as_lists_in_tables=False,
                accept_naive_datetimes=False,
                datetime_tzinfo=None,
                serializer_by_class={},
                deserializer_by_udt={}
            )
            
            _original_print = print
            def print(*args, **kwargs):
                json_args = []
                for arg in args:
                    json_args.append(_json_module.dumps(preprocess_table_payload(arg, _serdes_options, None), separators=(',', ':')))
                _original_print(*json_args, **kwargs)
            """ + content;
        }

        val env = switch (ctx.connectionInfo().destination()) {
            case ASTRA -> "prod";
            case ASTRA_DEV -> "dev";
            case ASTRA_TEST -> "test";
            case DSE -> "dse";
            case HCD -> "hcd";
            case CASSANDRA -> "cassandra";
            case OTHERS -> "other";
        };

        content = """
        import astrapy as _astrapy_module
        import astrapy.utils.unset as _astrapy_unset_module
        
        _original_init = _astrapy_module.DataAPIClient.__init__
        
        def patched_init(self, *args, **kwargs):
            if "environment" not in kwargs or isinstance(kwargs["environment"], _astrapy_unset_module.UnsetType):
                kwargs["environment"] = "%s"
            return _original_init(self, *args, **kwargs)
        
        _astrapy_module.DataAPIClient.__init__ = patched_init
        """.formatted(env) + content;

        return content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom().run(execEnv.envDir(), "./.venv/bin/python", "-m", "mypy", "example.py", "--show-error-codes", "--pretty", "--show-column-numbers");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom().run(execEnv.envDir(), envVars, ".venv/bin/python", execEnv.scriptPath());
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val result = ExternalPrograms.custom().run(execEnv.envDir(), ".venv/bin/pip", "list", "--format", "json");
        
        if (result.notOk()) {
            throw new RuntimeException("Failed to extract Python client version: " + result.output());
        }

        try {
            val packages = JacksonUtils.parseJson(result.stdout(), ArrayNode.class);
            
            for (val pkg : packages) {
                if ("astrapy".equals(pkg.get("name").asString())) {
                    return Optional.of("v" + pkg.get("version").asString());
                }
            }
        } catch (Exception _) {}

        return Optional.of("unknown");
    }
}
