package com.dtsx.dh.core.docs.runner;

import com.dtsx.dh.core.common.ClientLanguage;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/// A collection of execution environments, one for each client language.
///
/// Provides access to environments by language and handles any cleanup of all environments if ran with the `--clean` flag.
///
/// @see ExecutionEnvironment
@RequiredArgsConstructor
public class ExecutionEnvironments {
    private final Map<ClientLanguage, ExecutionEnvironment> map;

    /// Gets the execution environment for a specific language.
    ///
    /// @param lang the client language
    /// @return the execution environment for that language
    public ExecutionEnvironment forLanguage(ClientLanguage lang) {
        return map.get(lang);
    }
}
