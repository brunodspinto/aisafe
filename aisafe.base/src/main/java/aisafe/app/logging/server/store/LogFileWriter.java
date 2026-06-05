package aisafe.app.logging.server.store;

import aisafe.app.logging.server.model.RemoteAccessEvent;

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

    /**
     * Opens (creating if needed) the given file for appending.
     *
     * @param file the log file path
     * @throws IOException if the file cannot be opened
     */
    public LogFileWriter(final Path file) throws IOException {
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Appends one event and flushes immediately so nothing is lost if the process is killed.
     *
     * @param event the event to persist
     */
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
