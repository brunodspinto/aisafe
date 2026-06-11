package aisafe.infrastructure.application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class AppSettings {

    private static final String PROPERTIES_RESOURCE     = "application.properties";
    private static final String REPOSITORY_FACTORY_KEY  = "persistence.repositoryFactory";
    private static final String FLIGHT_TESTER_BINARY_KEY = "flight.tester.binary";
    private static final String SIMULATION_REPORT_FILE_KEY = "simulation.report.file";

    private final Properties properties = new Properties();

    public AppSettings() {
        try (var stream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_RESOURCE)) {
            if (stream == null)
                throw new FileNotFoundException("application.properties not found in classpath");
            properties.load(stream);
        } catch (final IOException ex) {
            System.err.println("[AppSettings] WARNING: Could not load " + PROPERTIES_RESOURCE
                    + " – " + ex.getMessage() + ". Falling back to InMemory factory.");
            properties.setProperty(REPOSITORY_FACTORY_KEY,
                    "aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory");
        }
    }

    public String getRepositoryFactory() {
        return properties.getProperty(REPOSITORY_FACTORY_KEY,
                "aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory");
    }

    /**
     * Returns the file-system path to the {@code flight_tester} C binary used by US085.
     * Defaults to {@code aisafe.base/simulation/flight_tester} if the property is not set.
     *
     * @return the configured or default binary path
     */
    public String flightTesterBinary() {
        return properties.getProperty(FLIGHT_TESTER_BINARY_KEY,
                "aisafe.base/simulation/flight_tester");
    }

    /**
     * Returns the file-system path to the simulation results file ({@code simulation_report.txt})
     * produced by the SCOMP/C simulation and read by US111. Defaults to {@code simulation_report.txt}
     * (the simulation's working directory) if the property is not set.
     *
     * @return the configured or default simulation report file path
     */
    public String simulationReportFile() {
        return properties.getProperty(SIMULATION_REPORT_FILE_KEY, "simulation_report.txt");
    }
}
