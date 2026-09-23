package com.dtsx.dh.core.docs.runner.tests;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class DuplicatesFinder {
    private static final Pattern LANG_FILE_PATTERN = Pattern.compile("^(?!shared\\.)\\w+\\.approved\\.txt$");

    public record Duplicates(List<Duplicate> unwrap) {
        public int totalCount() {
            return unwrap.stream().mapToInt(d -> d.languages().size()).sum();
        }
    }

    public record Duplicate(Path testPath, List<String> languages) {}

    @SneakyThrows
    public Duplicates findDuplicates(Path examplesRoot) {
        val duplicates = new ArrayList<Duplicate>();

        try (val paths = Files.walk(examplesRoot)) {
            paths.filter(Files::isDirectory)
                .forEach(dir -> checkDirectoryForDuplicates(examplesRoot, dir, duplicates));
        }

        return new Duplicates(duplicates);
    }

    @SneakyThrows
    private void checkDirectoryForDuplicates(Path snapshotsRoot, Path dir, List<Duplicate> duplicates) {
        val sharedFile = dir.resolve("shared.approved.txt");

        if (!Files.exists(sharedFile)) {
            return;
        }

        val sharedContent = Files.readString(sharedFile);
        val sharedByteSize = sharedContent.getBytes(StandardCharsets.UTF_8).length;
        val matchingLanguages = new ArrayList<String>();

        try (val files = Files.list(dir)) {
            files.filter(f -> LANG_FILE_PATTERN.matcher(f.getFileName().toString()).matches())
                .forEach(langFile -> checkFileForDuplicate(langFile, sharedContent, sharedByteSize, matchingLanguages));
        }

        if (!matchingLanguages.isEmpty()) {
            val relativePath = snapshotsRoot.relativize(dir);
            duplicates.add(new Duplicate(relativePath, matchingLanguages));
        }
    }

    @SneakyThrows
    private void checkFileForDuplicate(Path langFile, String sharedContent, long sharedByteSize, List<String> matchingLanguages) {
        if (Files.size(langFile) != sharedByteSize) {
            return;
        }

        val langContent = Files.readString(langFile);

        if (sharedContent.equals(langContent)) {
            val language = langFile.getFileName().toString().replace(".approved.txt", "");
            matchingLanguages.add(language);
        }
    }
}
