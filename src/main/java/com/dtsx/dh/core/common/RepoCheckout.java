package com.dtsx.dh.core.common;

import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.val;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Performs fresh shallow git checkouts of client repos.
public class RepoCheckout {
    /// The GitHub org every client repo lives under.
    public static final String DATASTAX_ORG = "datastax";

    /// Clones a client repo fresh and shallow into `.dh_temp/client_repos/<lang>/`.
    ///
    /// Always deletes the target directory first, then clones via init+fetch so a branch, tag or
    /// commit SHA all work through the same path. A spec pointing at an existing local checkout
    /// never goes through here at all - it's used as-is, with its git state untouched.
    public static Path checkout(ExternalProgram git, Path targetDir, String repo, String ref) {
        deleteIfExists(targetDir);

        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new CliException("Failed to create " + targetDir, e);
        }

        val url = "https://github.com/" + repo + ".git";

        git.runOrThrow("init", "-q", targetDir.toString());
        git.runOrThrow("-C", targetDir.toString(), "remote", "add", "origin", url);
        git.runOrThrow("-C", targetDir.toString(), "fetch", "--depth", "1", "--no-tags", "origin", ref);
        git.runOrThrow("-C", targetDir.toString(), "-c", "advice.detachedHead=false", "checkout", "-q", "FETCH_HEAD");

        return targetDir;
    }

    private static void deleteIfExists(Path dir) {
        try {
            if (Files.exists(dir)) {
                FileUtils.deleteDirectory(dir.toFile());
            }
        } catch (IOException e) {
            throw new CliException("Failed to delete existing clone at " + dir, e);
        }
    }
}
