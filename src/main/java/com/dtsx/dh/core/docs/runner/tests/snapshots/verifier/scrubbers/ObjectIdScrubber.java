package com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.scrubbers;

import org.approvaltests.scrubbers.RegExScrubber;

import java.util.regex.Pattern;

public class ObjectIdScrubber extends RegExScrubber {
    public static final Pattern OBJECT_ID_PATTERN = Pattern.compile("[a-fA-F0-9]{24}");

    public ObjectIdScrubber() {
        super(OBJECT_ID_PATTERN, "objectId");
    }
}
