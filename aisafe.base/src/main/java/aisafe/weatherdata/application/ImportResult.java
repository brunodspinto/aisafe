package aisafe.weatherdata.application;

import java.util.Collections;
import java.util.List;

/**
 * Value object summarising the outcome of a bulk weather data import.
 *
 * <p>Carries the count of successfully persisted records and an unmodifiable list
 * of human-readable failure messages, one per rejected row (AC042.5).</p>
 */
public class ImportResult {

    private final int saved;
    private final List<String> failures;

    public ImportResult(final int saved, final List<String> failures) {
        this.saved = saved;
        this.failures = Collections.unmodifiableList(failures);
    }

    /** @return the number of records successfully persisted */
    public int saved() {
        return saved;
    }

    /** @return unmodifiable list of failure messages, one per rejected row */
    public List<String> failures() {
        return failures;
    }
}
