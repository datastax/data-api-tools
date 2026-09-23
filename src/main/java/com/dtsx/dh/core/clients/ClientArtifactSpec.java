package com.dtsx.dh.core.clients;

import com.dtsx.dh.core.common.RepoCheckout;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/// Parses a `-R/--repo` `SPEC` for a client suite into either a local checkout path, or a repo and
/// ref to clone.
///
/// An existing local path is used as-is. Otherwise a leading `@` means the whole remainder is a ref
/// against the suite's default repo (`@my-branch`); otherwise the spec splits at the first `@` only,
/// so ref names containing `/` survive (`someone/astra-db-go@feature/foo` is repo
/// `someone/astra-db-go`, ref `feature/foo`). A bare repo name (no `/`) defaults to the
/// [RepoCheckout#DATASTAX_ORG] org.
public sealed interface ClientArtifactSpec {
    record LocalPath(Path path) implements ClientArtifactSpec {}

    record Remote(String repo, String ref) implements ClientArtifactSpec {}

    /// Resolves a `-R/--repo` value into a [ClientArtifactSpec]: `raw` parsed if present, otherwise
    /// the default repo and ref.
    static ClientArtifactSpec resolve(Optional<String> raw, String defaultRepo, String defaultRef) {
        return raw
            .map((s) -> parse(s, defaultRepo, defaultRef))
            .orElseGet(() -> new Remote(qualify(defaultRepo), defaultRef));
    }

    private static ClientArtifactSpec parse(String raw, String defaultRepo, String defaultRef) {
        val asPath = Path.of(raw);

        if (Files.exists(asPath)) {
            return new LocalPath(asPath);
        }

        if (raw.startsWith("@")) {
            return new Remote(qualify(defaultRepo), raw.substring(1));
        }

        val at = raw.indexOf('@');

        if (at < 0) {
            return new Remote(qualify(raw), defaultRef);
        }

        return new Remote(qualify(raw.substring(0, at)), raw.substring(at + 1));
    }

    private static String qualify(String repo) {
        return repo.contains("/") ? repo : RepoCheckout.DATASTAX_ORG + "/" + repo;
    }
}
