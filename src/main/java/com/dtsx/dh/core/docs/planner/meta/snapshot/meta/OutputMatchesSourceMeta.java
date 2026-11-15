package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import lombok.NonNull;

import java.util.regex.Pattern;

public record OutputMatchesSourceMeta(@NonNull Pattern regex) {}
