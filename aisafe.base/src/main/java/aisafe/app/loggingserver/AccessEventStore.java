package aisafe.app.loggingserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory store for remote-access events (shared by US90 and US91).
 * Keeps up to MAX_EVENTS recent events (FIFO) and tracks active sessions.
 */
public final class AccessEventStore {

    private static final int MAX_EVENTS = 500;

    private static final AccessEventStore INSTANCE = new AccessEventStore();

    private final CopyOnWriteArrayList<AccessEvent> events = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, AccessEvent> activeSessions = new ConcurrentHashMap<>();

    private AccessEventStore() {}

    public static AccessEventStore getInstance() {
        return INSTANCE;
    }

    public void addEvent(final AccessEvent event) {
        if (event == null) return;

        events.add(event);
        if (events.size() > MAX_EVENTS) {
            events.remove(0);
        }

        final String type = event.getEventType();
        if ("LOGIN_SUCCESS".equals(type)) {
            activeSessions.put(event.getUsername(), event);
        } else if ("LOGOUT".equals(type) || "CONNECTION_LOST".equals(type)) {
            activeSessions.remove(event.getUsername());
        }
    }

    /**
     * Returns the last {@code max} events in chronological order (oldest first).
     */
    public List<AccessEvent> getRecentEvents(final int max) {
        final List<AccessEvent> snapshot = new ArrayList<>(events);
        final int from = Math.max(0, snapshot.size() - max);
        return Collections.unmodifiableList(snapshot.subList(from, snapshot.size()));
    }

    /**
     * Returns an unmodifiable view of currently active sessions (username → login event).
     */
    public Map<String, AccessEvent> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }
}
