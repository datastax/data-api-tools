package com.dtsx.dh.core.docs.planner.meta.compiles;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.docs.planner.fixtures.NoopFixture;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig.SkipTestType;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestType;
import com.dtsx.dh.core.docs.runner.PlaceholderVars;
import com.dtsx.dh.core.common.ClientLanguage;
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
