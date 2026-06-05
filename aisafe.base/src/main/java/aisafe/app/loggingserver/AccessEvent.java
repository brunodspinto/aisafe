package aisafe.app.loggingserver;

/**
 * Immutable record representing one remote-access event received by the logging server.
 * Payload format (pipe-delimited, as produced by RemoteAccessLogger):
 *   2026-06-04 14:32:01 | joana | 127.0.0.1 | 54321 | US78 | LOGIN_SUCCESS
 */
public final class AccessEvent {

    private final String timestamp;
    private final String username;
    private final String clientIp;
    private final int clientPort;
    private final String serviceId;
    private final String eventType;

    public AccessEvent(final String timestamp, final String username, final String clientIp,
                       final int clientPort, final String serviceId, final String eventType) {
        this.timestamp = timestamp;
        this.username = username;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.serviceId = serviceId;
        this.eventType = eventType;
    }

    /**
     * Parses a pipe-delimited line into an AccessEvent.
     *
     * @param line the raw payload string
     * @return parsed event, or {@code null} if the format is invalid
     */
    public static AccessEvent parse(final String line) {
        if (line == null) return null;
        final String[] parts = line.split("\\s*\\|\\s*");
        if (parts.length < 6) return null;
        try {
            return new AccessEvent(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim(),
                    Integer.parseInt(parts[3].trim()),
                    parts[4].trim(),
                    parts[5].trim()
            );
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    public String getTimestamp()  { return timestamp; }
    public String getUsername()   { return username; }
    public String getClientIp()   { return clientIp; }
    public int    getClientPort() { return clientPort; }
    public String getServiceId()  { return serviceId; }
    public String getEventType()  { return eventType; }

    @Override
    public String toString() {
        return timestamp + " | " + username + " | " + clientIp + " | " + clientPort
                + " | " + serviceId + " | " + eventType;
    }
}
