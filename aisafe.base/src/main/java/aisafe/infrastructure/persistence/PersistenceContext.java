package aisafe.infrastructure.persistence;

import aisafe.infrastructure.application.AiSafeApplication;

public final class PersistenceContext {

    private static RepositoryFactory theFactory;

    public static RepositoryFactory repositories() {
        if (theFactory == null) {
            final String factoryClassName = AiSafeApplication.settings().getRepositoryFactory();
            try {
                theFactory = (RepositoryFactory) Class.forName(factoryClassName)
                        .getDeclaredConstructor().newInstance();
            } catch (final ReflectiveOperationException ex) {
                throw new IllegalStateException(
                        "Unable to load RepositoryFactory: " + factoryClassName, ex);
            }
        }
        return theFactory;
    }

    private PersistenceContext() {}
}
