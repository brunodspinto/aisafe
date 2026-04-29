package aisafe.infrastructure.application;

public final class AiSafeApplication {

    private static final AppSettings SETTINGS = new AppSettings();

    public static AppSettings settings() {
        return SETTINGS;
    }

    private AiSafeApplication() {}
}
