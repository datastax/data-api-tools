package com.dtsx.dh.commands.completions;

import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.VerifyMode;
import com.dtsx.dh.core.docs.runner.tests.reporter.TestReporters;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgramType;
import lombok.val;
import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.List;

@Command(
    name = "compgen",
    description = "Generate a completions script for the CLI",
    mixinStandardHelpOptions = true
)
public class CompgenCmd implements Runnable {
    @Spec
    protected CommandSpec spec;

    @Override
    public final void run() {
        var script = AutoComplete.bash(spec.root().name(), spec.root().commandLine());

        val enums = List.of(
            VerifyMode.class,
            ClientLanguage.class,
            TestReporters.class,
            ExternalProgramType.class
        );

        for (val clazz : enums) {
            for (val constant : clazz.getEnumConstants()) {
                script = script.replace("\"" + constant.name() + "\"", "\"" + constant.name().toLowerCase() + "\"");
            }
        }

        System.out.println(script);
    }
}
