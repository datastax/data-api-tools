package com.dtsx.dh.core.common;

import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.clients.impls.CSharpSuite;
import com.dtsx.dh.core.clients.impls.GoSuite;
import com.dtsx.dh.core.clients.impls.JavaSuite;
import com.dtsx.dh.core.clients.impls.PythonSuite;
import com.dtsx.dh.core.clients.impls.TypeScriptSuite;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.drivers.impls.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/// Represents the various client languages available in enum form.
///
/// Useful, as `enum`s come with parsing and holding static information for free.
///
/// @see ClientDriver
@Getter
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(
        ".java",
        "\"com.datastax.astra:astra-db-java:2.+\"",
        JavaDriver::new,
        JavaSuite::new
    ),
    PYTHON(
        ".py",
        "astrapy",
        PythonDriver::new,
        PythonSuite::new
    ),
    TYPESCRIPT(
        ".ts",
        "@datastax/astra-db-ts",
        TypeScriptDriver::new,
        TypeScriptSuite::new
    ),
    CSHARP(
        ".cs",
        "<PackageReference Include=\"DataStax.AstraDB.DataApi\" Version=\"2.*-*\"/>",
        CSharpDriver::new,
        CSharpSuite::new
    ),
    GO(
        ".go",
        "github.com/datastax/astra-db-go/v2@main",
        GoDriver::new,
        GoSuite::new
    ),
    BASH(
        ".sh",
        null,
        BashDriver::new,
        null
    );

    private final String extension;
    private final @Nullable String defaultArtifact;
    private final Function<String, ClientDriver> mkDocsDriver;

    /// Factory for this language's [ClientSuite], or `null` for languages `dh clients test` doesn't
    /// support: `bash` (no client repo behind it) and any client language not yet wired up.
    private final @Nullable Supplier<ClientSuite> mkClientSuite;

    public @Nullable String defaultArtifact() {
        return defaultArtifact;
    }

    public static List<String> names() {
        return Arrays.stream(ClientLanguage.values()).map(Enum::name).toList();
    }
}
