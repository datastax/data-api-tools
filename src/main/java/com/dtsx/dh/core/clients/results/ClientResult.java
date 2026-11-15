package com.dtsx.dh.core.clients.results;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/// The result of running a single client's integration suite: whether it passed, how long it took,
/// and where its log landed.
///
/// `logFile` is `null` for a client that was skipped, since nothing ran to produce one.
public record ClientResult(Outcome outcome, Duration duration, Optional<Path> logFile) {}
