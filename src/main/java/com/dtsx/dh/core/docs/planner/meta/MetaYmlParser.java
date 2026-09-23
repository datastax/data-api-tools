package com.dtsx.dh.core.docs.planner.meta;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.PlanException;
import com.dtsx.dh.core.docs.planner.meta.compiles.CompilesTestMeta;
import com.dtsx.dh.core.docs.planner.meta.compiles.CompilesTestMetaRep;
import com.dtsx.dh.core.docs.planner.meta.snapshot.SnapshotTestMeta;
import com.dtsx.dh.core.docs.planner.meta.snapshot.SnapshotTestMetaRep;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;
import tools.jackson.core.JacksonException;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dtsx.dh.core.docs.runner.tests.VerifyMode.COMPILE_ONLY;

public class MetaYmlParser {
    public static BaseMetaYml parseMetaYml(TestCtx ctx, Path ymlFile) {
        val rep = parseRep(ymlFile);

        validateSchemaPath(rep, ymlFile);

        if (rep.test().type() != rep.expectTestType()) {
            throw new PlanException("'" + ymlFile + "' was parsed as a '" + rep.expectTestType() + "' test descriptor, but was actually a '" + rep.test().type() + "' test descriptor");
        }

        try {
            if (ctx.verifyMode() == COMPILE_ONLY) {
                return new CompilesTestMeta(ctx, rep); // Dependent on the invariant that all snapshot tests can be run as compilation tests
            }

            return switch (rep) {
                case SnapshotTestMetaRep m -> new SnapshotTestMeta(ctx, ymlFile.getParent(), m);
                case CompilesTestMetaRep m -> new CompilesTestMeta(ctx, m);
                default -> throw new RuntimeException(); // unreachable
            };
        } catch (Exception e) {
            throw new PlanException("Failed to parse meta.yml file at '" + ymlFile + "': " + e.getMessage(), e);
        }
    }

    private static BaseMetaYml.BaseMetaYmlRep parseRep(Path file) {
        try {
            return JacksonUtils.parseYaml(file, SnapshotTestMetaRep.class);
        } catch (JacksonException se) {
            try {
                return JacksonUtils.parseYaml(file, CompilesTestMetaRep.class);
            } catch (JacksonException ce) {
                throw new PlanException("Failed to parse meta.yml file at '" + file + "'; errors:\n" +
                    "- SnapshotTestMetaYml: " + se.getMessage() + "\n" +
                    "- CompilesTestMetaYml: " + ce.getMessage());
            }
        }
    }

    private static void validateSchemaPath(BaseMetaYml.BaseMetaYmlRep rep, Path ymlFile) {
        val schemaPath = ymlFile.getParent().resolve(rep.$schema());

        if (!Files.exists(schemaPath)) {
            throw new PlanException("Invalid $schema path for '" + ymlFile + "'");
        }
    }
}
