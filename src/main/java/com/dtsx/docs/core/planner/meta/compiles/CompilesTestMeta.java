package com.dtsx.docs.core.planner.meta.compiles;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.fixtures.NoopFixture;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig.SkipTestType;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestType;
import com.dtsx.docs.core.runner.PlaceholderVars;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import lombok.Getter;
import tools.jackson.core.type.TypeReference;

import java.util.Map;

@Getter
public final class CompilesTestMeta implements BaseMetaYml {
    private final SkipConfig skipConfig;
    private final PlaceholderVars vars;

    public CompilesTestMeta(TestCtx ctx, BaseMetaYmlRep meta) {
        this.skipConfig = SkipConfig.parse((Map<ClientLanguage, SkipTestType> l) -> new SkipConfig(TestType.COMPILES, l), ctx, meta.test().skip(), new TypeReference<>() {});
        this.vars = meta.test().vars().orElse(PlaceholderVars.EMPTY);
    }

    @Override
    public JSFixture baseFixture() {
        return NoopFixture.COMPILATION_TESTS_INSTANCE;
    }
}
