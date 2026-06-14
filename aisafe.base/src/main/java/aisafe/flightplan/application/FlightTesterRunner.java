package aisafe.flightplan.application;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy for invoking the flight tester and returning its stdout.
 * The production implementation delegates to the {@code flight_tester} C binary;
 * unit tests inject a stub that returns a fixed JSON string without spawning a process.
 */
@FunctionalInterface
interface FlightTesterRunner {
    /**
     * Runs the flight tester against the given JSON input file and returns the full stdout.
     *
     * @param jsonFile path to the temporary JSON input file
     * @return the C binary's stdout as a string
     * @throws IOException      if the process cannot be started or the output cannot be read
     * @throws RuntimeException if the process times out or is forcibly terminated
     */
    String run(Path jsonFile) throws IOException;
}
