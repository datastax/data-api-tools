package com.dtsx.dh.core.docs.planner.fixtures;

import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoopFixture extends JSFixture {
    public static final NoopFixture SNAPSHOT_TESTS_INSTANCE = new NoopFixture("no base fixture");
    public static final NoopFixture COMPILATION_TESTS_INSTANCE = new NoopFixture("compilation tests");

    private final String name;

    @Override
    public String fixtureName() {
        return name;
    }

    @Override
    public FixtureMetadata meta(ExternalProgram tsx, FixtureIndex index) {
        return FixtureMetadata.emptyForIndex(index);
    }

    @Override
    public void setup(ExternalProgram tsx, FixtureMetadata md) {
        // noop
    }

    @Override
    public void beforeEach(ExternalProgram tsx, FixtureMetadata md, @Nullable ClientLanguage lang) {
        // noop
    }

    @Override
    public void afterEach(ExternalProgram tsx, FixtureMetadata md, @Nullable ClientLanguage lang) {
        // noop
    }

    @Override
    public void teardown(ExternalProgram tsx, FixtureMetadata md) {
        // noop
    }
}
