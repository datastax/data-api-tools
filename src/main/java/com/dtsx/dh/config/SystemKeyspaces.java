package com.dtsx.dh.config;

import java.util.Set;

/// Keyspaces that `dh db clean` must never touch.
///
/// HCD returns these alongside user keyspaces when listing keyspaces; Astra doesn't return
/// any of them. `system` and anything prefixed `system_` are matched by name, since new
/// `system_*` keyspaces show up across HCD/DSE versions; the set below covers the ones that
/// don't follow that naming.
public class SystemKeyspaces {
    private static final Set<String> EXCLUDED = Set.of(
        "data_endpoint_auth"
    );

    public static boolean isSystemKeyspace(String keyspace) {
        return keyspace.equals("system") || keyspace.startsWith("system_") || EXCLUDED.contains(keyspace);
    }
}
