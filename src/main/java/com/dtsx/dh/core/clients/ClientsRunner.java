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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/// Orchestrates a `dh clients test` run against an already-built [ClientsPlan]: preparing each
/// client's repo (concurrently), bootstrapping the target database, running each selected client's
/// own suite in turn, and writing its log.
public class ClientsRunner {
    public static boolean runSuites(ClientsTestCtx ctx, ClientsPlan plan) {
        if (ctx.clean()) {
            deleteManagedClones(ctx);
        }

        val logsDir = CliLogger.runLogsDir(ctx);

        prepareRepos(ctx, plan);
        bootstrapDatabase(ctx);

        val results = new LinkedHashMap<ClientLanguage, ClientResult>();
        var bailed = false;

        ClientsReporter.printRunningHeader();

        for (val lang : plan.languages()) {
            if (bailed) {
                val result = new ClientResult(Outcome.SKIPPED, Duration.ZERO, null);
                results.put(lang, result);
                ClientsReporter.printClientResult(lang, result);
                continue;
            }

            val result = runClient(ctx, plan, lang, logsDir);
            results.put(lang, result);
            ClientsReporter.printClientResult(lang, result);

            if (result.outcome() == Outcome.FAIL && ctx.bail()) {
                bailed = true;
            }
        }

        ClientsReporter.printSummary(results);

        return results.values().stream().noneMatch((r) -> r.outcome() == Outcome.FAIL);
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

    private static void prepareRepos(ClientsTestCtx ctx, ClientsPlan plan) {
        val git = ExternalPrograms.git(ctx);
        val total = plan.languages().size();
        val completed = new AtomicInteger(0);

        CliLogger.loading(ClientsReporter.preparingReposMessage(0, total), (update) -> {
            val tasks = new ArrayList<Runnable>();

            for (val lang : plan.languages()) {
                val entry = plan.entry(lang);

                tasks.add(() -> {
                    if (entry.repoSpec() instanceof Remote remote) {
                        RepoCheckout.checkout(git, entry.repoDir(), remote.repo(), remote.ref());
                    }

                    entry.suite().setup(ctx, entry.repoDir());

                    val done = completed.incrementAndGet();
                    update.update((msg) -> ClientsReporter.preparingReposMessage(done, total));
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

    private static void bootstrapDatabase(ClientsTestCtx ctx) {
        val admin = DataAPIUtils.getDatabaseAdmin(ctx.connectionInfo());
        val existing = admin.listKeyspaceNames();

        CliLogger.loading(ClientsReporter.bootstrappingMessage(), (_) -> {
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
        val label = lang.name().toLowerCase();

        if (entry.needsWipe()) {
            CliLogger.loading(ClientsReporter.wipingMessage(label), (_) -> {
                wipeContentsFor(ctx);
                return null;
            });
        }

        val logFile = resolveLogPath(logsDir, label);

        val logWriter = openLogWriter(logFile, entry);

        val start = Instant.now();

        RunResult result = null;

        try {
            result = CliLogger.loading(ClientsReporter.runningMessage(label), (_) ->
                ExternalPrograms.custom().run(entry.invocation().cwd(), entry.invocation().env(), (line) -> appendToLog(logWriter, line), entry.invocation().cmd().toArray(new String[0]))
            );
        } finally {
            closeLogWriter(logWriter, result);
        }

        val duration = Duration.between(start, Instant.now());

        val outcome = entry.suite().isSuccess(result) ? Outcome.PASS : Outcome.FAIL;

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
    /// potentially concurrently, so writes are synchronized on `writer`.
    private static void appendToLog(@Nullable BufferedWriter writer, OutputLine line) {
        if (writer == null) {
            return;
        }

        synchronized (writer) {
            try {
                writer.write(line.unwrap());
                writer.flush();
            } catch (IOException e) {
                CliLogger.exception(e);
            }
        }
    }

    /// Appends the `exit code:` line, when the process got far enough to produce one, and closes
    /// `writer`.
    private static void closeLogWriter(@Nullable BufferedWriter writer, @Nullable RunResult result) {
        if (writer == null) {
            return;
        }

        synchronized (writer) {
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
}
