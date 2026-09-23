package com.dtsx.dh.core.clients;

import com.datastax.astra.client.core.options.DataAPIClientOptions;
import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.core.clients.ClientArtifactSpec.Remote;
import com.dtsx.dh.core.clients.reporter.ClientsReporter;
import com.dtsx.dh.core.clients.results.ClientResult;
import com.dtsx.dh.core.clients.results.Outcome;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.common.RepoCheckout;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.DataAPIUtils;
import com.dtsx.dh.lib.ExecutorUtils;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.OutputLine;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.KeyspaceOps;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/// Prepares each selected client's repo and, once run with `-y`, runs its suite against the target
/// database: bootstrapping the database, running each selected client's own suite in turn against an
/// already-built [ClientsPlan], and writing its log.
public class ClientsRunner {
    private static final ClientResult BAILED_RESULT = new ClientResult(Outcome.SKIPPED, Duration.ZERO, null);

    public static boolean runSuites(ClientsTestCtx ctx, ClientsPlan plan) {
        val logsDir = CliLogger.runLogsDir(ctx);

        bootstrapDatabase(ctx);

        val results = new LinkedHashMap<ClientLanguage, ClientResult>();
        var bailed = false;

        ClientsReporter.printRunningHeader();

        for (val lang : plan.languages()) {
            val result = (!bailed)
                ? runClient(ctx, plan, lang, logsDir)
                : BAILED_RESULT;

            results.put(lang, result);
            ClientsReporter.printClientResult(lang, result);

            if (result.outcome() == Outcome.FAIL && ctx.bail()) {
                bailed = true;
            }
        }

        ClientsReporter.printSummary(results);

        return results.values().stream().noneMatch((r) -> r.outcome() == Outcome.FAIL);
    }

    /// Prepares every selected language's repo: deletes the managed clones first when `--clean` was
    /// passed, then checks out (or reuses a local path for) and runs [ClientSuite#setup] on each,
    /// concurrently. Runs unconditionally, including on a dry run, so [ClientsPlan#build] can resolve
    /// every suite's real invocation.
    public static void prepareRepos(ClientsTestCtx ctx) {
        if (ctx.clean()) {
            deleteManagedClones(ctx);
        }

        val git = ExternalPrograms.git(ctx);
        val total = ctx.languages().size();
        val completed = new AtomicInteger(0);

        CliLogger.loading("Preparing client repos...", (update) -> {
            val tasks = new ArrayList<Runnable>();

            for (val lang : ctx.languages()) {
                val repoSpec = ctx.repoSpec(lang);
                val repoDir = ClientsPlan.resolveRepoDir(ctx, lang, repoSpec);
                val suite = ctx.suite(lang);

                tasks.add(() -> {
                    if (repoSpec instanceof Remote(var repo, var ref)) {
                        RepoCheckout.checkout(git, repoDir, repo, ref);
                    }

                    suite.setup(ctx, repoDir);

                    val done = completed.incrementAndGet();
                    update.update((_) -> "Preparing client repos (@!" + done + "/" + total + "!@)...");
                });
            }

            try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
                val futures = ExecutorUtils.emptyFuturesList();
                tasks.forEach((task) -> futures.add(executor.submit(task)));
                ExecutorUtils.awaitAll(futures);
            }

            return null;
        });
    }

    private static void deleteManagedClones(ClientsTestCtx ctx) {
        val clonesDir = ctx.tmpFolder().resolve("client_repos");

        try {
            if (Files.exists(clonesDir)) {
                FileUtils.deleteDirectory(clonesDir.toFile());
            }
        } catch (IOException e) {
            throw new CliException("Failed to delete managed clones at " + clonesDir, e);
        }
    }

    private static void bootstrapDatabase(ClientsTestCtx ctx) {
        val admin = DataAPIUtils.getDatabaseAdmin(ctx.connectionInfo());
        val existing = admin.listKeyspaceNames();

        CliLogger.loading("Ensuring @!default_keyspace!@ exists...", (_) -> {
            KeyspaceOps.ensureKeyspace(admin, DataAPIClientOptions.DEFAULT_KEYSPACE, existing);
            return null;
        });
    }

    private static void wipeContentsFor(ClientsTestCtx ctx) {
        val admin = DataAPIUtils.getDatabaseAdmin(ctx.connectionInfo());
        KeyspaceOps.wipeAllContents(ctx.connectionInfo(), admin);
    }

    private static ClientResult runClient(ClientsTestCtx ctx, ClientsPlan plan, ClientLanguage lang, Path logsDir) {
        val entry = plan.entry(lang);
        val langName = lang.name().toLowerCase();

        if (entry.needsWipe()) {
            CliLogger.loading("Wiping target database for @!" + langName + "!@...", (_) -> {
                wipeContentsFor(ctx);
                return null;
            });
        }

        val logFile = resolveLogPath(logsDir, langName);
        val logWriter = openLogWriter(logFile, entry);

        val start = Instant.now();
        RunResult result = null;

        try {
            result = CliLogger.loading("Running @!" + langName + "!@ integration tests...", (_) ->
                ExternalPrograms.custom().run(entry.invocation().cwd(), entry.invocation().env(), (line) -> appendToLog(logWriter, line), entry.invocation().cmd().toArray(new String[0]))
            );
        } finally {
            closeLogWriter(logWriter, result);
        }

        val duration = Duration.between(start, Instant.now());

        val outcome = result.ok()
            ? Outcome.PASS
            : Outcome.FAIL;

        return new ClientResult(outcome, duration, logFile);
    }

    /// Resolves the log path for a client, creating its parent directory.
    private static Path resolveLogPath(Path logsDir, String label) {
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            throw new CliException("Failed to create log directory " + logsDir, e);
        }

        return logsDir.resolve("clients-" + label + ".log");
    }

    /// Opens `logFile`, writing the `cwd`/`cmd` header immediately so a `tail -f` shows context
    /// before the client produces any output. Never writes `env`, which may hold secrets.
    ///
    /// Returns `null` if the file could not be opened, in which case log writes are silently
    /// skipped for the rest of the run.
    private static @Nullable BufferedWriter openLogWriter(Path logFile, ClientsPlan.Entry entry) {
        try {
            val writer = Files.newBufferedWriter(logFile, UTF_8, CREATE, TRUNCATE_EXISTING);
            writer.write("cwd: " + entry.invocation().cwd() + "\n");
            writer.write("cmd: " + String.join(" ", entry.invocation().cmd()) + "\n\n");
            writer.flush();
            return writer;
        } catch (IOException e) {
            CliLogger.exception(e);
            return null;
        }
    }

    /// Appends one line of a client's output to its log file, flushing immediately so a `tail -f`
    /// stays live.
    ///
    /// Called from both the stdout- and stderr-reading threads of [ExternalPrograms.ExternalProgram#run],
    /// potentially concurrently, so writes are serialized.
    private static synchronized void appendToLog(@Nullable BufferedWriter writer, OutputLine line) {
        if (writer == null) {
            return;
        }

        try {
            writer.write(line.unwrap());
            writer.flush();
        } catch (IOException e) {
            CliLogger.exception(e);
        }
    }

    /// Appends the `exit code:` line, when the process got far enough to produce one, and closes
    /// `writer`.
    private static synchronized void closeLogWriter(@Nullable BufferedWriter writer, @Nullable RunResult result) {
        if (writer == null) {
            return;
        }

        try {
            if (result != null) {
                writer.write("\nexit code: " + result.exitCode() + "\n");
            }

            writer.close();
        } catch (IOException e) {
            CliLogger.exception(e);
        }
    }
}
