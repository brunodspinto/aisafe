package aisafe.weatherdata.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@code ImportResult}.
 */
class ImportResultTest {

    @Test
    void ensureImportResultReportsCorrectSavedCount() {
        final ImportResult result = new ImportResult(3, List.of());
        assertEquals(3, result.saved());
    }

    @Test
    void ensureImportResultReportsFailures() {
        final List<String> failures = List.of(
                "Row 2: unknown area code 'XX'",
                "Row 4: invalid wind direction");
        final ImportResult result = new ImportResult(1, failures);

        assertEquals(1, result.saved());
        assertEquals(2, result.failures().size());
        assertTrue(result.failures().contains("Row 2: unknown area code 'XX'"));
    }

    @Test
    void ensureImportResultWithZeroSavedAndNoFailures() {
        final ImportResult result = new ImportResult(0, List.of());
        assertEquals(0, result.saved());
        assertTrue(result.failures().isEmpty());
    }
}
