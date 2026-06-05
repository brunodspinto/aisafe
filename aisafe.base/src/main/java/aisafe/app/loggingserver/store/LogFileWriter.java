package aisafe.app.loggingserver.store;

import aisafe.app.loggingserver.model.RemoteAccessEvent;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Appends each recorded event to a local text file (one pipe-delimited line per event),
 * giving the log durability across restarts. The file uses the same format as the wire
 * payload, so it can be re-parsed on startup with {@code LogEventParser}.
 */
public final class LogFileWriter implements Closeable {

    private final BufferedWriter writer;

    public LogFileWriter(final Path file) throws IOException {
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized void append(final RemoteAccessEvent event) {
        try {
            writer.write(event.toLogLine());
            writer.newLine();
            writer.flush();
        } catch (final IOException ex) {
            System.err.println("[US90] Could not write log file: " + ex.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
