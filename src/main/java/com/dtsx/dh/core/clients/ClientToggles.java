package com.dtsx.dh.core.clients;

/// The four test toggles shared by every client suite.
public record ClientToggles(boolean vectorize, boolean exhaustiveVectorize, boolean reranking, boolean admin) {}
