package com.dtsx.docs.core.runner.tests.snapshots.verifier;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotsShareConfig;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import lombok.Getter;
import lombok.val;
import org.approvaltests.approvers.FileApprover;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.writers.Writer;

import java.io.File;

public class ExampleResultNamer implements ApprovalNamer {
    @Getter
    private final String exampleName;
    private final String groupName;
    private final ClientLanguage language;
    private final TestCtx ctx;

    static {
        FileApprover.tracker.addAllowedDuplicates((_) -> true);
    }

    public ExampleResultNamer(TestCtx ctx, ClientLanguage language, TestRoot testRoot, SnapshotsShareConfig shareConfig) {
        this.exampleName = testRoot.rootName();

        this.groupName = (!shareConfig.isShared(language))
            ? language.name().toLowerCase()
            : "shared";

        this.language = language;
        this.ctx = ctx;
    }

    @Override
    public String getApprovalName() {
        return exampleName + "/_snapshots/" + groupName;
    }

    @Override
    public String getSourceFilePath() {
        return ctx.examplesFolder().toAbsolutePath().toString();
    }

    @Override
    public File getReceivedFile(String extensionWithDot) {
        val lang = groupName.equals("shared")
            ? "." + language.name().toLowerCase()
            : "";

        return new File(getSourceFilePath() + "/" + getApprovalName() + lang + Writer.received + extensionWithDot);
    }

    @Override
    public File getApprovedFile(String extensionWithDot) {
        return new File(getSourceFilePath() + "/" + getApprovalName() + Writer.approved + extensionWithDot);
    }

    @Override
    public ApprovalNamer addAdditionalInformation(String additionalInformation) {
        return this; // we can just ignore this
    }

    @Override
    public String getAdditionalInformation() {
        return ""; // we can just ignore this
    }
}
