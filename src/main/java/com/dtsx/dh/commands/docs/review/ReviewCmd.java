package com.dtsx.dh.commands.docs.review;

import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.core.docs.runner.RunException;
import com.dtsx.dh.lib.CliLogger;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Command(
    name = "review",
    description = "Start the snapshot review dashboard"
)
public class ReviewCmd extends BaseCmd<ReviewCtx> {
    @Mixin @Getter
    private ReviewArgs $args;

    @Override
    public int run() {
        CliLogger.println(false, "@|bold Starting snapshot review dashboard...|@");
        CliLogger.println(false);

        try {
            ensureDependenciesInstalled();
            clearLogs();
            startDashboard();
            return 0;
        } catch (Exception e) {
            CliLogger.exception("Failed to start dashboard: " + e.getMessage(), e);
            return 1;
        }
    }

    private void ensureDependenciesInstalled() {
        CliLogger.println(false, "@|bold Installing dashboard dependencies...|@");

        CliLogger.loading("Installing dependencies", (_) -> {
            val installProcess = new ProcessBuilder()
                .command("npm", "install")
                .directory(ctx.dashboardFolder().toFile())
                .redirectErrorStream(true)
                .start();

            try (val reader = new BufferedReader(new InputStreamReader(installProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    CliLogger.println(false, line);
                }
            }

            val exitCode = installProcess.waitFor();
            if (exitCode != 0) {
                throw new RunException("npm install failed with exit code: " + exitCode);
            }
            return null;
        });

        CliLogger.println(false);
    }

    private void clearLogs() {
        val logFile = ctx.tmpFolder().resolve("dashboard.log").toFile();

        if (logFile.exists()) {
            if (!logFile.delete()) {
                CliLogger.println(false, "@|yellow Warning: Failed to clear old dashboard logs|@");
            }
        }
    }

    private void startDashboard() throws Exception {
        val processBuilder = new ProcessBuilder()
            .command("npm", "run", "dev")
            .directory(ctx.dashboardFolder().toFile());

        val env = processBuilder.environment();
        env.put("EXAMPLES_DIR", ctx.examplesFolder().toAbsolutePath().toString());
        env.put("PORT", String.valueOf(ctx.port()));

        if (ctx.detached()) {
            startDetached(processBuilder);
        } else {
            startAttached(processBuilder);
        }
    }

    private void startDetached(ProcessBuilder processBuilder) throws Exception {
        val logFile = ctx.tmpFolder().resolve("dashboard.log").toFile();

        processBuilder
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
            .redirectError(ProcessBuilder.Redirect.appendTo(logFile));

        val dashboardProcess = processBuilder.start();

        Thread.sleep(2000);
        
        if (!dashboardProcess.isAlive()) {
            throw new RunException("Dashboard process failed to start");
        }

        val url = "http://localhost:" + ctx.port();
        
        CliLogger.println(false, "@|bold Dashboard started in background|@");
        CliLogger.println(false, "@!-!@ URL: @!@|cyan,underline " + url + "|@!@");
        CliLogger.println(false, "@!-!@ PID: @!" + dashboardProcess.pid() + "!@");
        CliLogger.println(false, "@!-!@ Logs: @!" + logFile.getAbsolutePath() + "!@");
        CliLogger.println(false);
        CliLogger.println(false, "@|bold To stop:|@");
        CliLogger.println(false, "@!$!@ kill " + dashboardProcess.pid());
        
        if (ctx.openBrowser()) {
            openBrowser(url);
        }
    }

    private void startAttached(ProcessBuilder processBuilder) throws Exception {
        processBuilder.inheritIO();

        val dashboardProcess = processBuilder.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (dashboardProcess.isAlive()) {
                CliLogger.println(false);
                CliLogger.println(false, "@|bold Shutting down dashboard...|@");
                dashboardProcess.destroy();
                try {
                    dashboardProcess.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    dashboardProcess.destroyForcibly();
                }
            }
        }));
        
        Thread.sleep(2000);
        
        val url = "http://localhost:" + ctx.port();
        
        CliLogger.println(false, "@|bold Dashboard running|@");
        CliLogger.println(false, "@!-!@ URL: @!@|underline " + url + "|@!@");
        CliLogger.println(false, "@!-!@ Examples: @!" + ctx.examplesFolder().toAbsolutePath() + "!@");
        CliLogger.println(false);
        CliLogger.println(false, "@|bold Press Ctrl+C to stop|@");
        CliLogger.println(false);
        
        if (ctx.openBrowser()) {
            openBrowser(url);
        }
        
        val exitCode = dashboardProcess.waitFor();
        
        if (exitCode != 0 && exitCode != 143) {
            throw new Exception("Dashboard process exited with code: " + exitCode);
        }
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception _) {}
    }
}
