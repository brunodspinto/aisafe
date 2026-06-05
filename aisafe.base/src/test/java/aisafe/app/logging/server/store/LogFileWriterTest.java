package aisafe.app.logging.server.store;

import aisafe.app.logging.server.model.RemoteAccessEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link LogFileWriter} (US090): events are appended as pipe-delimited lines
 * in the same format as the wire payload (so the file can be reloaded on startup).
 */
class LogFileWriterTest {

    @Test
    void ensureEventIsAppendedAsPipeDelimitedLine(@TempDir final Path dir) throws Exception {
        final Path file = dir.resolve("logs.txt");
        final RemoteAccessEvent event = new RemoteAccessEvent(
                LocalDateTime.of(2026, 6, 4, 17, 0, 0),
                "atcc1", "10.0.0.5", 50231, "US78", "LOGIN_SUCCESS", "127.0.0.1");

        try (LogFileWriter writer = new LogFileWriter(file)) {
            writer.append(event);
        }

        final List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size());
        assertEquals("2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS", lines.get(0));
    }
}
