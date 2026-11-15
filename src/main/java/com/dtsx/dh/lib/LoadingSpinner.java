package com.dtsx.dh.lib;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.function.FailableRunnable;

import java.util.Optional;

import static com.dtsx.dh.lib.ColorUtils.stripAnsi;

// Vendored from Astra CLI
public class LoadingSpinner {
    private final String[] SPINNER_FRAMES = new String[]{ "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏" };

    private static final int FRAME_DELAY_MS = 80;

    @Getter
    private volatile String message;
    private int lastLineLength = 0;

    private volatile boolean active = false;
    private volatile boolean paused = false;

    private final Object activityLock = new Object();

    public LoadingSpinner() {
        Thread.ofVirtual()
            .name("loading-spinner")
            .start(this::runSpinner);
    }

    public Optional<LoadingSpinnerControls> start(String message) {
        synchronized (activityLock) {
            if (active) {
                return Optional.empty();
            }

            this.message = message;
            this.active = true;

            activityLock.notifyAll();
        }
        return Optional.of(new LoadingSpinnerControls());
    }

    public class LoadingSpinnerControls {
        public void updateMessage(String newMessage) {
            message = newMessage;
        }

        public void stop() {
            active = false;
            paused = false;
            clearLine();
        }
    }

    public Runnable pause() {
        synchronized (activityLock) {
            paused = true;
            clearLine();
        }

        return () -> {
            paused = false;
            synchronized (activityLock) {
                activityLock.notifyAll();
            }
        };
    }

    @SuppressWarnings("InfiniteLoopStatement") // could add shutdown flag later if needed
    private void runSpinner() {
        var frameIndex = 0;

        while (true) {
            synchronized (activityLock) {
                while (!active || paused) {
                    catchInterrupt(activityLock::wait);
                }
            }

            clearLine();

            val currentLine = ColorUtils.highlight(SPINNER_FRAMES[frameIndex]) + " " + message + "...";

            synchronized (activityLock) {
                System.err.print(currentLine);
                System.err.flush();
            }

            lastLineLength = stripAnsi(currentLine).length();
            frameIndex = (frameIndex + 1) % SPINNER_FRAMES.length;

            catchInterrupt(() -> Thread.sleep(FRAME_DELAY_MS));
        }
    }

    private void clearLine() {
        synchronized (activityLock) {
            if (lastLineLength > 0) {
                System.err.print("\r" + " ".repeat(lastLineLength) + "\r");
                System.err.flush();
                lastLineLength = 0;
            }
        }
    }

    private void catchInterrupt(FailableRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            CliLogger.exception(e);
            Thread.currentThread().interrupt();
        }
    }
}
