package com.dtsx.docs.core.planner.meta.snapshot;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.fixtures.JSFixtureImpl;
import com.dtsx.docs.core.planner.fixtures.NoopFixture;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig.SkipTestType;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestType;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaRep.FixturesConfig;
import com.dtsx.docs.core.runner.PlaceholderVars;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import lombok.Getter;
import lombok.val;
import tools.jackson.core.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dtsx.docs.core.runner.tests.VerifyMode.DRY_RUN;
import static com.dtsx.docs.lib.Constants.DEFAULT_TEST_FIXTURE;
import static com.dtsx.docs.lib.Constants.FIXTURES_DIR;

@Getter
public final class SnapshotTestMeta implements BaseMetaYml {
    private final SkipConfig skipConfig;
    private final JSFixture baseFixture;
    private final JSFixture testFixture;
    private final List<SnapshotSource> snapshotSources;
    private final SnapshotsShareConfig shareConfig;
    private final ExecutionMode executionMode;
    private final PlaceholderVars vars;

    public SnapshotTestMeta(TestCtx ctx, Path testRoot, SnapshotTestMetaRep meta) {
        this.skipConfig = SkipConfig.parse((Map<ClientLanguage, SkipTestType> l) -> new SkipConfig(TestType.SNAPSHOT, l), ctx, meta.test().skip(), new TypeReference<>() {});
        this.baseFixture = resolveBaseFixture(ctx, meta.fixtures().flatMap(FixturesConfig::base));
        this.testFixture = resolveTestFixture(ctx, testRoot);
        this.snapshotSources = SnapshotSourcesParser.parseSources(meta.snapshots());
        this.shareConfig = SnapshotsShareConfig.parse(SnapshotsShareConfig::new, ctx, meta.snapshots().share(), new TypeReference<>() {});
        this.executionMode = meta.execution().orElse(ExecutionMode.SEQUENTIAL);
        this.vars = meta.test().vars().orElse(PlaceholderVars.EMPTY);
    }

    /// Resolves a base fixture from the `_fixtures/` directory.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   _fixtures/
    ///     basic-collection.js  <- resolves "basic-collection.js"
    /// ```
    ///
    /// @param ctx the verifier context
    /// @param fixtureName the optional name of the base fixture file
    /// @return the resolved [JSFixture]
    /// @throws PlanException if the fixture doesn't exist
    ///
    /// @see JSFixture
    private static JSFixture resolveBaseFixture(TestCtx ctx, Optional<String> fixtureName) {
        if (fixtureName.isEmpty()) {
            return NoopFixture.SNAPSHOT_TESTS_INSTANCE;
        }

        val path = ctx.examplesFolder().resolve(FIXTURES_DIR).resolve(fixtureName.get());

        if (!Files.exists(path)) {
            throw new PlanException("Base fixture '" + fixtureName.get() + "' does not exist in '" + FIXTURES_DIR + "'");
        }

        return mkJsFixtureImpl(ctx, path);
    }

    /// Resolves a test-specific fixture from the test root directory.
    ///
    /// Looks for `fixture.js` in the test root, if it exists; otherwise returns a no-op fixture.
    ///
    /// Example:
    /// ```
    /// examples/
    ///   dates/
    ///    fixture.js      <- resolves this file
    ///    meta.yml
    ///   delete-many/
    ///    (no fixture.js) <- resolves no-op fixture
    ///    meta.yml
    /// ```
    ///
    /// @param ctx the verifier context
    /// @param testRoot the test root directory
    /// @return the resolved [JSFixture] (or no-op if fixture.js doesn't exist)
    ///
    /// @see JSFixture
    private static JSFixture resolveTestFixture(TestCtx ctx, Path testRoot) {
        val path = testRoot.resolve(DEFAULT_TEST_FIXTURE);

        if (!Files.exists(path)) {
            return NoopFixture.SNAPSHOT_TESTS_INSTANCE;
        }

        return mkJsFixtureImpl(ctx, path);
    }

    private static JSFixture mkJsFixtureImpl(TestCtx ctx, Path path) {
        return new JSFixtureImpl(ctx, path, ctx.verifyMode() == DRY_RUN);
    }
}
