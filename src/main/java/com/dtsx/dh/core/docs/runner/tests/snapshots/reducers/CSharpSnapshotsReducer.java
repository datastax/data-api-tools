package com.dtsx.dh.core.docs.runner.tests.snapshots.reducers;

import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.Snapshot;
import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.Snapshot.SnapshotPart;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO don't love this there should be a better way to do this
public enum CSharpSnapshotsReducer implements SnapshotsReducer {
    INSTANCE;

    @Override
    public Snapshot reduceSnapshots(Map<Snapshot, Set<Path>> snapshots) throws SnapshotReductionException {
        if (snapshots.size() == 1) {
            return snapshots.keySet().iterator().next();
        }
        
        if (snapshots.size() == 2) {
            val iter = snapshots.entrySet().iterator();
            val e1 = iter.next();
            val e2 = iter.next();

            val snapshot1 = e1.getKey();
            val snapshot2 = e2.getKey();

            val untypedSnapshot = isUntyped(snapshot1) ? snapshot1 : snapshot2;

            if (firstSnapshotSubsetOfSecond(snapshot1.parts(), snapshot2.parts()) || firstSnapshotSubsetOfSecond(snapshot2.parts(), snapshot1.parts())) {
                return untypedSnapshot;
            }

            throw new SnapshotReductionException();
        }

        throw new SnapshotReductionException();
    }

    private boolean isUntyped(Snapshot snapshot) {
        return snapshot.parts().stream()
            .filter(p -> p.name().endsWith("::jsonify") || p.name().endsWith("::definition") || p.name().endsWith("::definitions"))
            .allMatch(p -> {
                val json = JacksonUtils.parseJson(p.content(), Object.class);
                return !hasPascalCaseKeys(json);
            });
    }

    private boolean hasPascalCaseKeys(Object json) {
        if (json instanceof Map<?, ?> m) {
            return m.keySet().stream().anyMatch(k -> k instanceof String s && Character.isUpperCase(s.charAt(0)));
        }
        if (json instanceof Iterable<?> i) {
            for (val item : (Iterable<?>) i) {
                if (hasPascalCaseKeys(item)) return true;
            }
        }
        return false;
    }

    private boolean firstSnapshotSubsetOfSecond(List<SnapshotPart> parts1, List<SnapshotPart> parts2) {
        if (parts1.size() != parts2.size()) {
            return false;
        }

        for (int i = 0; i < parts1.size(); i++) {
            val part1 = parts1.get(i);
            val part2 = parts2.get(i);

            if (!part1.name().equals(part2.name())) {
                return false;
            }

            if (part1.name().endsWith("::jsonify") || part1.name().endsWith("::definition") || part1.name().endsWith("::definitions")) {
                val typedJson = JacksonUtils.parseJson(part1.content(), Object.class);
                val untypedJson = JacksonUtils.parseJson(part2.content(), Object.class);

                if (!isSubset(typedJson, untypedJson)) {
                    return false;
                }
            } else {
                if (!part1.content().equals(part2.content())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isSubset(Object typed, Object untyped) {
        if (typed == null) return true;
        if (untyped == null) return false;

        if (typed instanceof Map<?, ?> tm && untyped instanceof Map<?, ?> um) {
            for (val e : tm.entrySet()) {
                val typedKey = e.getKey();

                val match = um.entrySet().stream()
                    .filter(ue -> keysMatch(typedKey, ue.getKey()))
                    .findFirst();
                
                if (match.isEmpty() || !isSubset(e.getValue(), match.get().getValue())) {
                    return false;
                }
            }
            return true;
        }
        
        if (typed instanceof Iterable<?> ti && untyped instanceof Iterable<?> ui) {
            val tit = ti.iterator();
            val uit = ui.iterator();
            while (tit.hasNext() && uit.hasNext()) {
                if (!isSubset(tit.next(), uit.next())) return false;
            }
            return !tit.hasNext() && !uit.hasNext();
        }
        
        return typed.equals(untyped);
    }

    private boolean keysMatch(Object key1, Object key2) {
        if (key1 instanceof String s1 && key2 instanceof String s2) {
            return normalizeStr(s1).equals(normalizeStr(s2));
        }
        return key1 != null && key1.equals(key2);
    }
    
    private String normalizeStr(String s) {
        return s.toLowerCase().replace("_", "");
    }
}
