package aisafe.app.loggingserver.store;

import aisafe.app.loggingserver.model.ActiveUser;
import aisafe.app.loggingserver.model.RemoteAccessEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe in-memory store of remote-access events.
 *
 * <p>This is the shared seam between US090 (the UDP receiver thread, which writes) and
 * US091 (the HTTP server threads, which read). A single instance is created by
 * {@code RemoteAccessLoggingServerApp} and shared by both.
 *
 * <ul>
 *   <li>{@link #recent(int)} backs the US091 "last recorded events" page (newest first).</li>
 *   <li>{@link #activeUsers()} backs the US091 "currently active users" page.</li>
 * </ul>
 */
public final class RemoteAccessLogStore {

    /** All events, newest first. */
    private final Deque<RemoteAccessEvent> events = new ConcurrentLinkedDeque<>();

    /** Active sessions keyed by {@link RemoteAccessEvent#sessionKey()}. */
    private final Map<String, RemoteAccessEvent> activeSessions = new ConcurrentHashMap<>();

    public void add(final RemoteAccessEvent event) {
        events.addFirst(event);
        switch (event.event()) {
            case "LOGIN_SUCCESS" -> activeSessions.put(event.sessionKey(), event);
            case "LOGOUT", "CONNECTION_LOST" -> activeSessions.remove(event.sessionKey());
            default -> { /* LOGIN_FAILED and any unknown event: not an active session */ }
        }
    }

    public List<RemoteAccessEvent> recent(final int limit) {
        final List<RemoteAccessEvent> out = new ArrayList<>();
        for (final RemoteAccessEvent e : events) {
            if (out.size() >= limit) break;
            out.add(e);
        }
        return out;
    }

    public List<RemoteAccessEvent> all() {
        return new ArrayList<>(events);
    }

    public List<ActiveUser> activeUsers() {
        final List<ActiveUser> out = new ArrayList<>();
        for (final RemoteAccessEvent e : activeSessions.values()) {
            out.add(new ActiveUser(e.username(), e.clientIp(), e.clientPort(), e.service(), e.timestamp()));
        }
        out.sort(Comparator.comparing(ActiveUser::since).reversed());
        return out;
    }

    public int size() {
        return events.size();
    }

    /** Clears all state. Intended for tests. */
    public void clear() {
        events.clear();
        activeSessions.clear();
    }
}
