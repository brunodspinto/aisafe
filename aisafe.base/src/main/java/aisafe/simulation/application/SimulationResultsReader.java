package aisafe.simulation.application;

import aisafe.simulation.domain.SimulationResults;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads the raw outcome of a simulation run from an external source (e.g. the
 * {@code simulation_report.txt} produced by the SCOMP/C simulation) and exposes it as a
 * {@link SimulationResults}. Implementations encapsulate the output format, keeping the domain
 * and application layers decoupled from the C side (Protected Variations / DIP).
 */
public interface SimulationResultsReader {

    /**
     * Reads and parses the simulation results from the given source file.
     *
     * @param source path to the simulation output file
     * @return the parsed {@link SimulationResults}
     * @throws IOException if the source cannot be read
     */
    SimulationResults read(Path source) throws IOException;
}
