package aisafe.reporting.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists generated reports as text files.
 */
public final class ReportWriter {

    private final Path outputDirectory;

    public ReportWriter() {
        this(Path.of("target", "reports"));
    }

    ReportWriter(final Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }
        this.outputDirectory = outputDirectory;
    }

    public Path write(final String fileName, final String content) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank.");
        }
        if (content == null) {
            throw new IllegalArgumentException("Report content cannot be null.");
        }

        try {
            Files.createDirectories(outputDirectory);
            final Path outputFile = outputDirectory.resolve(fileName);
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);
            return outputFile;
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to write report file.", e);
        }
    }
}
