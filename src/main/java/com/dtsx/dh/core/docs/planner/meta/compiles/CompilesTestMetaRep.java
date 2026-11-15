package com.dtsx.dh.core.docs.planner.meta.compiles;

import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep;
import lombok.NonNull;

import static com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestType.COMPILES;

public record CompilesTestMetaRep(
    @NonNull String $schema,
    @NonNull TestBlock test
) implements BaseMetaYmlRep {
    @Override
    public TestType expectTestType() {
        return COMPILES;
    }
}
