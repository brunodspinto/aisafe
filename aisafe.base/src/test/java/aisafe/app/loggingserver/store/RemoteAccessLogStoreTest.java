package aisafe.app.loggingserver.store;

import aisafe.app.loggingserver.model.ActiveUser;
import aisafe.app.loggingserver.model.RemoteAccessEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteAccessLogStoreTest {

    private static final String IP = "10.0.0.5";
    private static final int PORT = 50000;

    private static RemoteAccessEvent ev(final String user, final String event, final int minute) {
        return new RemoteAccessEvent(
                LocalDateTime.of(2026, 6, 4, 17, minute, 0),
                user, IP, PORT, "US78", event, "127.0.0.1");
    }

    @Test
    void ensureRecentReturnsNewestFirst() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        store.add(ev("a", "LOGIN_SUCCESS", 1));
        store.add(ev("b", "LOGIN_SUCCESS", 2));
        store.add(ev("c", "LOGIN_SUCCESS", 3));

        final List<RemoteAccessEvent> recent = store.recent(10);

        assertEquals("c", recent.get(0).username());
        assertEquals("b", recent.get(1).username());
        assertEquals("a", recent.get(2).username());
    }

    @Test
    void ensureRecentRespectsTheLimit() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        for (int i = 1; i <= 5; i++) {
            store.add(ev("u" + i, "LOGIN_SUCCESS", i));
        }
        assertEquals(2, store.recent(2).size());
        assertEquals(5, store.size());
    }

    @Test
    void ensureLoginSuccessMakesUserActive() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        store.add(ev("atcc1", "LOGIN_SUCCESS", 1));

        final List<ActiveUser> active = store.activeUsers();
        assertEquals(1, active.size());
        assertEquals("atcc1", active.get(0).username());
        assertEquals("US78", active.get(0).service());
    }

    @Test
    void ensureLogoutRemovesActiveUser() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        store.add(ev("atcc1", "LOGIN_SUCCESS", 1));
        store.add(ev("atcc1", "LOGOUT", 2));

        assertTrue(store.activeUsers().isEmpty());
        assertEquals(2, store.size(), "logout is still recorded in history");
    }

    @Test
    void ensureConnectionLostRemovesActiveUser() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        store.add(ev("atcc1", "LOGIN_SUCCESS", 1));
        store.add(ev("atcc1", "CONNECTION_LOST", 2));

        assertTrue(store.activeUsers().isEmpty());
    }

    @Test
    void ensureFailedLoginIsRecordedButNotActive() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        store.add(ev("hacker", "LOGIN_FAILED", 1));

        assertTrue(store.activeUsers().isEmpty());
        assertEquals(1, store.size());
    }

    @Test
    void ensureStoreIsCappedAtMaxEvents() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        for (int i = 0; i <= RemoteAccessLogStore.MAX_EVENTS + 10; i++) {
            store.add(ev("u" + i, "LOGIN_FAILED", i % 60));
        }
        assertEquals(RemoteAccessLogStore.MAX_EVENTS, store.size());
    }

    @Test
    void ensureClearResetsEventsAndActiveUsers() {
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        store.add(ev("a", "LOGIN_SUCCESS", 1));

        store.clear();

        assertEquals(0, store.size());
        assertTrue(store.activeUsers().isEmpty());
    }
}
