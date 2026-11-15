package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

import java.util.Optional;

public record OutputJsonifySourceMeta(
    @NonNull Optional<String> jq,
    @NonNull @JsonProperty("jq-bash") Optional<String> jqBash,
    @NonNull Optional<Boolean> sort
) {}
