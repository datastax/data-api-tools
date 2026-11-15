package com.dtsx.dh.config.args.mixins;

import picocli.CommandLine.Option;

/// Shared `-b/--bail` flag for commands that run through a series of items and can stop after the
/// first failure.
public class BailMixin {
    @Option(
        names = { "-b", "--bail" },
        description = "Whether to stop execution upon the first failure.",
        defaultValue = "${BAIL:-false}"
    )
    private boolean $bail;

    public boolean unwrap() {
        return $bail;
    }
}
