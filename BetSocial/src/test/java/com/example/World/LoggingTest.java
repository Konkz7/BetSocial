package com.example.World;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logging goes through a logger.
 *
 * There were eighteen System.out.println calls. Standard out has no level, so
 * nothing can be turned down; no timestamp beyond whatever the container adds;
 * no logger name, so there is no telling what wrote a line; and it is invisible
 * to anything that collects logs. One of them printed a Firebase push token,
 * which is a credential for sending to somebody's device.
 *
 * This is a source scan rather than a behavioural test because there is no
 * behaviour to observe - the point is that a particular call does not appear.
 * The alternative is noticing in review, which is how eighteen of them got in.
 */
@DisplayName("Logging")
class LoggingTest {

    private static final Path MAIN = Path.of("src", "main", "java");

    @Test
    @DisplayName("nothing writes to standard out")
    void noPrintlnInMainSources() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> sources = Files.walk(MAIN)) {
            for (Path file : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains("System.out.print") || line.contains("System.err.print")
                            || line.contains("printStackTrace()")) {
                        offenders.add(MAIN.relativize(file) + ":" + (i + 1) + " " + line.trim());
                    }
                }
            }
        }

        assertThat(offenders)
                .as("use a logger: standard out has no level, no logger name, and "
                        + "is invisible to anything that collects logs")
                .isEmpty();
    }

    @Test
    @DisplayName("the scan is actually looking at something")
    void scanFindsSources() throws IOException {
        // Without this, a wrong path would make the test above pass by reading no
        // files at all - which is the failure mode of every test that asserts an
        // absence.
        try (Stream<Path> sources = Files.walk(MAIN)) {
            assertThat(sources.filter(p -> p.toString().endsWith(".java")).count())
                    .as("the source scan found no files, so it proves nothing")
                    .isGreaterThan(50);
        }
    }
}
