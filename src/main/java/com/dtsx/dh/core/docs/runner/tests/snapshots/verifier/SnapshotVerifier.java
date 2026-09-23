package com.dtsx.dh.core.docs.runner.tests.snapshots.verifier;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.SnapshotsShareConfig;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome.FailedToVerify;
import com.dtsx.dh.core.docs.runner.tests.snapshots.reducers.SnapshotReductionException;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.scrubbers.ObjectIdScrubber;
import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.scrubbers.UUIDScrubber;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.ExecutionStrategy.TestResetter;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.MultiScrubber;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static com.dtsx.dh.core.docs.runner.tests.VerifyMode.DRY_RUN;
import static com.dtsx.dh.core.docs.runner.tests.VerifyMode.NORMAL;

@RequiredArgsConstructor
public class SnapshotVerifier {
    private static final String LAST_MODIFIED_FILE = "last-modified.txt";

    public static final Scrubber SCRUBBER = new MultiScrubber(List.of(
        new UUIDScrubber(),
        new ObjectIdScrubber()
    ));

    private final TestCtx ctx;
    private final List<SnapshotSource> snapshotSources;
    private final SnapshotsShareConfig shareConfig;

    @SneakyThrows
    @SuppressWarnings("BusyWait")
    public TestOutcome verify(ClientDriver driver, TestRoot testRoot, FixtureMetadata md, Set<Path> filesForLang, TestResetter resetter, Function<Path, RunResult> result) {
        if (ctx.verifyMode() == DRY_RUN) {
            return TestOutcome.DryPassed.INSTANCE;
        }

        var snapshots = new HashMap<Snapshot, Set<Path>>();

        for (val filePath : filesForLang) {
            for (var i = 0; true; i++) {
                try {
                    resetter.beforeEach().run();
                    val runResult = result.apply(filePath);
                    val fileSnapshot = mkSnapshot(driver, md, runResult);
                    snapshots.computeIfAbsent(fileSnapshot, _ -> new HashSet<>()).add(filePath);
                    break;
                } catch (Exception e) {
                    val lowercaseMessage = e.getMessage().toLowerCase();
                    if (i < 2 && (lowercaseMessage.contains("timeout") || lowercaseMessage.contains("timed out"))) {
                        CliLogger.exception("Retrying due to timeout when verifying file '" + filePath + "'", e);
                        Thread.sleep(1000);
                        continue;
                    }
                    throw e;
                } finally {
                    resetter.afterEach().run();
                }
            }
        }

        try {
            val snapshot = driver.language().snapshotsReducer().reduceSnapshots(snapshots);
            return verifySnapshot(driver, testRoot, snapshot);
        } catch (SnapshotReductionException e) {
            return TestOutcome.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.INSTANCE.alsoLog(testRoot, driver.language(), snapshots);
        }
    }

    private Snapshot mkSnapshot(ClientDriver driver, FixtureMetadata md, RunResult result) {
        val parts = snapshotSources.stream()
            .map(s -> s.mkSnapshot(ctx, driver, result, md))
            .toList();

        return Snapshot.fromParts(SCRUBBER, parts);
    }

    // TODO maybe cache approved test file contents' hashes + client artifacts so we don't re-verify unchanged tests?
    private TestOutcome verifySnapshot(ClientDriver driver, TestRoot testRoot, Snapshot snapshot) {
        val namer = mkNamer(driver.language(), testRoot);
        val options = mkApprovalOptions(namer);

        try {
            Approvals.verify(snapshot, options);
            return TestOutcome.Passed.INSTANCE;
        } catch (Error e) {
            val expectedPath = Optional.of(namer.getApprovedFile(".txt").toPath())
                .filter(Files::exists);

            // updates last-modified.txt when a new *.received.txt file is created
            // allows the reviewing dashboard to automatically detect changes in the snapshots
            if (ctx.verifyMode() == NORMAL) {
                updateLastModified(ctx.examplesFolder());
            }

            return new FailedToVerify(expectedPath).alsoLog(testRoot, driver.language(), snapshot);
        }
    }

    private Options mkApprovalOptions(ExampleResultNamer namer) {
        return new Options()
            .forFile().withNamer(namer)
            .withReporter((_, _) -> true)
            .and(ctx.verifyMode().applyOptions(ctx, namer.getApprovedFile(".txt").toPath()));
    }

    private ExampleResultNamer mkNamer(ClientLanguage language, TestRoot testRoot) {
        return new ExampleResultNamer(ctx, language, testRoot, shareConfig);
    }

    private static void updateLastModified(Path snapshotsFolder) {
        try {
            val lastModifiedPath = snapshotsFolder.resolve(LAST_MODIFIED_FILE);
            Files.writeString(lastModifiedPath, Instant.now().toString());
        } catch (IOException e) {
            CliLogger.exception("Failed to update " + LAST_MODIFIED_FILE + " in snapshots folder '" + snapshotsFolder + "'", e);
        }
    }
}
