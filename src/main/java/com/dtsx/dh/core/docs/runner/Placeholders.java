package com.dtsx.dh.core.docs.runner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Placeholders {
    protected Optional<String> collectionName = Optional.empty();

    protected Optional<String> tableName = Optional.empty();

    protected String keyspaceName = "default_keyspace";

    protected PlaceholderVars vars = new PlaceholderVars();
}
