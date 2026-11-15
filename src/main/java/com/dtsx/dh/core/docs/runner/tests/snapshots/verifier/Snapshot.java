package com.dtsx.dh.core.docs.runner.tests.snapshots.verifier;

import lombok.*;
import org.approvaltests.core.Scrubber;

import java.util.List;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Snapshot {
    private final List<SnapshotPart> parts;

    public static Snapshot fromParts(Scrubber scrubber, List<SnapshotPart> parts) {
        return new Snapshot(
            parts.stream()
                .map(p -> new SnapshotPart(p.name, scrubber.scrub(p.content)))
                .sorted()
                .toList()
        );
    }

    public record SnapshotPart(String name, String content) implements Comparable<SnapshotPart> {
        @Override
        public int compareTo(SnapshotPart other) {
            return this.name.compareTo(other.name); // ensures snapshot part ordering is always deterministic
        }
    }

    @Override
    public String toString() {
        val sb = new StringBuilder();

        for (val part : parts) {
            sb.append("---").append(part.name).append("---\n");
            sb.append(part.content).append("\n");
        }

        return sb.substring(0, sb.length() - 1);
    }
}
