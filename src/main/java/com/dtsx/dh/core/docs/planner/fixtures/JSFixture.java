package com.dtsx.dh.core.docs.planner.fixtures;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.dh.core.docs.planner.meta.snapshot.SnapshotTestMetaRep;
import com.dtsx.dh.core.docs.runner.RunException;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

import static com.dtsx.dh.HelperCli.CLI_DIR;

/// Represents a JavaScript fixture file used to set up, reset, and tear down database state for testing, with being JavaScript used for ease of scripting.
///
/// JS fixture files are of the following format, where each function is optional:
/// ```javascript
/// import * as $ from '../_base/prelude';
///
/// export async function Setup() {
///   // optional setup code
/// }
///
/// export async function Reset() {
///  // code to reset database state before each test
/// }
///
/// export async function Teardown() {
///  // optional teardown code
/// }
/// ```
///
/// There are two kinds of fixtures:
/// 1. The "base" fixture
///    - This fixture sets up major resources to be reused between a group of tests, such as a collection or keyspace.
///    - Defined in the `_fixtures/` directory of the examples folder.
///    - Referenced in {@link SnapshotTestMetaRep meta.yml} under `fixtures.base`.
/// 2. The "per-test" fixture
///    - This fixture sets up and resets data that is specific to each test, such as rows in a collection.
///    - Defined in the {@link com.dtsx.dh.core.docs.planner.TestRoot test root directory} of each example.
///    - Always called `fixture.js`
///
/// Example:
/// ```
/// examples/
///   _base/
///     prelude.js
///   _fixtures/
///     basic-collection.js <- base fixture
///   find-many/            <- test root
///     fixture.js          <- per-test fixture
///     meta.yml
/// ```
///
/// @see SnapshotTestMetaRep
/// @see TestRoot
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public sealed abstract class JSFixture implements Comparable<JSFixture> permits NoopFixture, JSFixtureImpl  {
    /// The name of the fixture, derived from its file name (if present)
    ///
    /// Equality is based on this name so that test roots using the same fixture can be grouped together.
    @EqualsAndHashCode.Include
    public abstract String fixtureName();

    public abstract FixtureMetadata meta(ExternalProgram tsx, FixtureIndex index);

    public abstract void setup(ExternalProgram tsx, FixtureMetadata md);
    public abstract void beforeEach(ExternalProgram tsx, FixtureMetadata md, @Nullable ClientLanguage lang);
    public abstract void afterEach(ExternalProgram tsx, FixtureMetadata md, @Nullable ClientLanguage lang);
    public abstract void teardown(ExternalProgram tsx, FixtureMetadata md);

    public static void installDependencies(TestCtx ctx) {
        CliLogger.debug("Installing JSFixture dependencies in " + Path.of(".").toAbsolutePath());

        val res = CliLogger.loading("Installing JS fixture dependencies", (_) -> {
            return ExternalPrograms.npm(ctx).run(CLI_DIR, "ci", "--prefer-offline"); // TODO use NPM_CONFIG_CACHE in CI
        });

        if (res.exitCode() != 0) {
            throw new RunException("Failed to install JS fixture dependencies: " + res.output());
        }
    }

    @Override
    public int compareTo(@NotNull JSFixture o) {
        val p1 = priority(this);
        val p2 = priority(o);

        if (p1 != p2) {
            return Integer.compare(p1, p2);
        }

        return o.fixtureName().compareTo(fixtureName());
    }

    // TODO:
    // potentially smarter sorting based on meta() so that fixtures creating heavy resources can be run first (or last??)
    // this would need meta() to be cached through so we aren't re-running JS to get the same metadata multiple times
    // but this might require meta() to be a static cache though in case two different but equal JSFixture instances
    // are created and compared???? can that happen??? Or should JSFixtures themselves be cached and only able to be created
    // through an opaque factory that returns existing instances if they already exist for a given path??? my head hurts.
    private static int priority(JSFixture f) {
        if (f == NoopFixture.COMPILATION_TESTS_INSTANCE) return 0;
        if (f == NoopFixture.SNAPSHOT_TESTS_INSTANCE) return 1;
        return 2;
    }
}
