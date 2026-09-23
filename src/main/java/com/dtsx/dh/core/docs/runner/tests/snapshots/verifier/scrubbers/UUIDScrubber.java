package com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.scrubbers;

import org.approvaltests.scrubbers.RegExScrubber;

import java.util.regex.Pattern;

public class UUIDScrubber extends RegExScrubber {
    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public UUIDScrubber() {
        super(UUID_PATTERN, "uuid");
    }
}
