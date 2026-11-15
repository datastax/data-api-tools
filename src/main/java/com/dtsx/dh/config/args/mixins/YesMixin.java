package com.dtsx.dh.config.args.mixins;

import picocli.CommandLine.Option;

/// Shared `-y/--yes` confirmation flag for commands that print a plan by default and only act on it
/// when explicitly told to.
public class YesMixin {
    @Option(
        names = { "-y", "--yes" },
        description = "Actually perform the operation. Without this, prints the plan and exits."
    )
    private boolean $yes;

    public boolean unwrap() {
        return $yes;
    }
}
