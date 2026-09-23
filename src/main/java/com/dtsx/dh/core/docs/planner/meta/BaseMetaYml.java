package com.dtsx.dh.core.docs.planner.meta;

import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig.SkipTestType;
import com.dtsx.dh.core.docs.runner.PlaceholderVars;
import com.dtsx.dh.core.common.ClientLanguage;
import lombok.NonNull;
import lombok.val;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Optional;

@JsonDeserialize()
public interface BaseMetaYml {
    JSFixture baseFixture();

    SkipConfig skipConfig();

    PlaceholderVars vars();

    interface BaseMetaYmlRep {
        String $schema();

        TestBlock test();

        TestType expectTestType();

        record TestBlock(@NonNull TestType type, @NonNull Optional<Object> skip, @NonNull Optional<PlaceholderVars> vars) {
            public static class SkipConfig extends PerLanguageToggle<SkipTestType> {
                public enum SkipTestType {
                    SNAPSHOT,
                    COMPILES,
                    ALL,
                    NONE
                }

                private final TestType type;

                public SkipConfig(TestType testType, Map<ClientLanguage, SkipTestType> languages) {
                    super(languages);
                    this.type = testType;
                }

                public boolean isSkipped(ClientLanguage language) {
                    val skipType = languages.getOrDefault(language, SkipTestType.NONE);

                    return switch (skipType) {
                        case ALL -> true;
                        case NONE -> false;
                        case SNAPSHOT -> type == TestType.SNAPSHOT;
                        case COMPILES -> type == TestType.COMPILES;
                    };
                }
            }
        }

        enum TestType {
            SNAPSHOT,
            COMPILES
        }
    }
}
