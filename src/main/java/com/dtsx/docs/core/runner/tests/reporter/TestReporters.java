package com.dtsx.docs.core.runner.tests.reporter;

import com.dtsx.docs.commands.docs.test.TestCtx;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public enum TestReporters {
    ONLY_FAILURES(OnlyFailuresReporter::new),
    ALL_TESTS(AllTestsReporter::new);

    private final Function<TestCtx, TestReporter> constructor;

    public TestReporter create(TestCtx ctx) {
        return constructor.apply(ctx);
    }
}
