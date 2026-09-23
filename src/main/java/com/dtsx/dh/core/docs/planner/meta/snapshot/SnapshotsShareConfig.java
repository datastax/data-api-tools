package com.dtsx.dh.core.docs.planner.meta.snapshot;

import com.dtsx.dh.core.docs.planner.meta.PerLanguageToggle;
import com.dtsx.dh.core.common.ClientLanguage;

import java.util.Map;

public class SnapshotsShareConfig extends PerLanguageToggle<Boolean> {
    public SnapshotsShareConfig(Map<ClientLanguage, Boolean> languages) {
        super(languages);
    }

    public boolean isShared(ClientLanguage language) {
        return languages.getOrDefault(language, true);
    }
}
