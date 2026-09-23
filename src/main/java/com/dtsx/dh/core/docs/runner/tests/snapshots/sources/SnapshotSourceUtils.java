package com.dtsx.dh.core.docs.runner.tests.snapshots.sources;

import com.dtsx.dh.core.docs.planner.PlanException;
import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.SnapshotVerifier;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

@UtilityClass
public class SnapshotSourceUtils {
    public static String extractOutput(String name, RunResult res) { // don't love this but at least it's simple
        if (name.startsWith("stdout")) {
            return res.stdout().trim();
        }
        if (name.startsWith("stderr")) {
            return res.stderr().trim();
        }
        throw new PlanException("Unexpected output stream: '" + name + "'");
    }

    // Sorts records returned by the Data API to ensure deterministic ordering for snapshot comparisons
    //
    // OK to use hash code as a comparator here since all values are "primitive" (or derived from primitives)
    // with strictly defined hash code computations even between different JVMs and runs
    //
    // Any deeper lists would already be returned deterministically by the Data API
    //
    // Any deeper maps will be sorted by SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS
    public static List<?> mkJsonDeterministic(List<?> records) {
        return (List<?>) mkJsonDeterministic((Object) records);
    }

    public static Object mkJsonDeterministic(Object obj) {
        if (obj == null) {
            return null;
        }

        return switch (obj) {
            case Map<?, ?> map -> {
                if (map.size() == 1) {
                    if (map.containsKey("$binary") || map.containsKey("_vector") || map.containsKey("embeddings")) {
                        yield "vector_or_binary";
                    }

                    if (map.containsKey("$date")) {
                        yield "date";
                    }
                }

                var result = new LinkedHashMap<>();
                map.entrySet().stream()
                    .map(e -> Pair.of(
                        mkJsonDeterministic(e.getKey()),
                        mkJsonDeterministic(e.getValue())
                    ))
                    .sorted(Comparator.comparing(p -> calcSortValue(p.getLeft()), Comparator.nullsFirst(Integer::compareTo)))
                    .forEach(p -> result.put(p.getLeft(), p.getRight()));
                yield result;
            }
            case Collection<?> coll -> {
                if (coll instanceof List<?> list && list.size() == 1024) {
                    if (list.stream().allMatch(e -> e instanceof Double)) {
                        yield "vector_or_binary";
                    }
                }

                yield coll.stream()
                    .map(SnapshotSourceUtils::mkJsonDeterministic)
                    .sorted(Comparator.comparing(SnapshotSourceUtils::calcSortValue, Comparator.nullsFirst(Integer::compareTo)))
                    .toList();
            }
            case Number num -> {
                if (num instanceof Float || num instanceof Double) {
                    if (num.doubleValue() % 1 == 0) {
                        yield num.longValue();
                    }
                }
                yield num;
            }
            default -> {
                yield obj;
            }
        };
    }

    public static void recursivelyPrintTypes(Object obj, String indent) {
        if (obj instanceof Map<?, ?> map) {
            System.out.println(indent + "Map:");
            for (var entry : map.entrySet()) {
                System.out.print(indent + "  Key (" + entry.getKey().getClass().getSimpleName() + "): ");
                recursivelyPrintTypes(entry.getKey(), indent + "    ");
                System.out.print(indent + "  Value (" + entry.getValue().getClass().getSimpleName() + "): ");
                recursivelyPrintTypes(entry.getValue(), indent + "    ");
            }
        } else if (obj instanceof Collection<?> coll) {
            System.out.println(indent + "List:");
            for (var item : coll) {
                System.out.print(indent + "  Item (" + item.getClass().getSimpleName() + "): ");
                recursivelyPrintTypes(item, indent + "    ");
            }
        } else {
            System.out.println(indent + obj.getClass().getSimpleName() + ": " + obj);
        }
    }

    private static int calcSortValue(Object obj) {
        if (obj == null) {
            return 0;
        }

        return switch (obj) {
            case Map<?, ?> map -> {
                yield map.entrySet().stream()
                    .mapToInt(e -> calcSortValue(e.getKey()) ^ calcSortValue(e.getValue()))
                    .sum();
            }
            case List<?> list -> {
                yield list.stream()
                    .mapToInt(SnapshotSourceUtils::calcSortValue)
                    .sum();
            }
            case Set<?> set -> {
                yield set.stream()
                    .mapToInt(SnapshotSourceUtils::calcSortValue)
                    .sum();
            }
            case String str -> {
                if (!SnapshotVerifier.SCRUBBER.scrub(str).equals(str)) {
                    yield 0;
                }
                yield pascalToSnakeCase(str).hashCode();
            }
            default -> {
                yield obj.hashCode();
            }
        };
    }

    private static String pascalToSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
