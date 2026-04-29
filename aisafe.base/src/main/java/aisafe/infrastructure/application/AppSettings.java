package aisafe.infrastructure.application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class AppSettings {

    private static final String PROPERTIES_RESOURCE = "application.properties";
    private static final String REPOSITORY_FACTORY_KEY = "persistence.repositoryFactory";

    private final Properties properties = new Properties();

    public AppSettings() {
        try (var stream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_RESOURCE)) {
            if (stream == null)
                throw new FileNotFoundException("application.properties not found in classpath");
            properties.load(stream);
        } catch (final IOException ex) {
            properties.setProperty(REPOSITORY_FACTORY_KEY,
                    "aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory");
        }
    }

    public String getRepositoryFactory() {
        return properties.getProperty(REPOSITORY_FACTORY_KEY,
                "aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory");
    }
}
