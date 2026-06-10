# US091 — Tests and Coverage

## Scope

US091 covers the HTTP visualization layer of the Remote Accesses Logging Server. The tests verify the parsing of UDP payloads, the thread-safe in-memory store, the file persistence, and the end-to-end UDP receive flow.

---

## Automated Tests

### `LogEventParserTest`

Location: `src/test/java/aisafe/app/loggingserver/udp/LogEventParserTest.java`

Unit tests for `LogEventParser`. Uses plain strings — no socket or network required.

**Test:** `ensureValidPayloadIsParsedIntoAllSixFields`

```java
@Test
void ensureValidPayloadIsParsedIntoAllSixFields() {
    final String payload = "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS";
    final Optional<RemoteAccessEvent> parsed = LogEventParser.parse(payload, "127.0.0.1");
    assertTrue(parsed.isPresent());
    assertEquals("atcc1", parsed.get().username());
    assertEquals("US78", parsed.get().service());
    assertEquals("LOGIN_SUCCESS", parsed.get().event());
}
```

**Test:** `ensureNullOrBlankPayloadIsRejected`

```java
@Test
void ensureNullOrBlankPayloadIsRejected() {
    assertTrue(LogEventParser.parse(null, "1.2.3.4").isEmpty());
    assertTrue(LogEventParser.parse("   ", "1.2.3.4").isEmpty());
}
```

**Test:** `ensureWrongNumberOfFieldsIsRejected`

```java
@Test
void ensureWrongNumberOfFieldsIsRejected() {
    assertTrue(LogEventParser.parse("only | three | fields", null).isEmpty());
    assertTrue(LogEventParser.parse("a | b | c | d | e | f | g", null).isEmpty());
}
```

**Test:** `ensureEmptyMandatoryFieldsAreRejected`

```java
@Test
void ensureEmptyMandatoryFieldsAreRejected() {
    // username empty
    assertTrue(LogEventParser.parse(
            "2026-06-04 17:00:00 |   | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS", null).isEmpty());
    // service empty
    assertTrue(LogEventParser.parse(
            "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 |   | LOGIN_SUCCESS", null).isEmpty());
    // event empty
    assertTrue(LogEventParser.parse(
            "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 | US78 |   ", null).isEmpty());
}
```

**Test:** `ensureBadTimestampFallsBackToNowButStillParses`

```java
@Test
void ensureBadTimestampFallsBackToNowButStillParses() {
    final String payload = "not-a-timestamp | atcc1 | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS";
    final Optional<RemoteAccessEvent> parsed = LogEventParser.parse(payload, null);
    assertTrue(parsed.isPresent());
    assertNotNull(parsed.get().timestamp());
}
```

**Test:** `ensureBadPortFallsBackToMinusOne`

```java
@Test
void ensureBadPortFallsBackToMinusOne() {
    final String payload = "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | NOT_A_PORT | US78 | LOGIN_SUCCESS";
    final Optional<RemoteAccessEvent> parsed = LogEventParser.parse(payload, null);
    assertTrue(parsed.isPresent());
    assertEquals(-1, parsed.get().clientPort());
}
```

**Test:** `ensureServiceIdentifierIsCarriedThroughForEachClient`

```java
@Test
void ensureServiceIdentifierIsCarriedThroughForEachClient() {
    assertEquals("US44", LogEventParser.parse(
            "2026-06-04 17:00:00 | wp1 | 10.0.0.1 | 100 | US44 | LOGIN_SUCCESS", null).get().service());
    assertEquals("US86", LogEventParser.parse(
            "2026-06-04 17:00:00 | pilot1 | 10.0.0.2 | 200 | US86 | LOGIN_SUCCESS", null).get().service());
}
```

---

### `RemoteAccessLogStoreTest`

Location: `src/test/java/aisafe/app/loggingserver/store/RemoteAccessLogStoreTest.java`

Unit tests for `RemoteAccessLogStore` — tests the business invariants of the shared in-memory store.

**Test:** `ensureRecentReturnsNewestFirst`

```java
@Test
void ensureRecentReturnsNewestFirst() {
    final RemoteAccessLogStore store = new RemoteAccessLogStore();
    store.add(ev("a", "LOGIN_SUCCESS", 1));
    store.add(ev("b", "LOGIN_SUCCESS", 2));
    store.add(ev("c", "LOGIN_SUCCESS", 3));
    final List<RemoteAccessEvent> recent = store.recent(10);
    assertEquals("c", recent.get(0).username());
    assertEquals("a", recent.get(2).username());
}
```

**Test:** `ensureRecentRespectsTheLimit`

```java
@Test
void ensureRecentRespectsTheLimit() {
    // add 5 events, ask for 2 — store still has 5
    assertEquals(2, store.recent(2).size());
    assertEquals(5, store.size());
}
```

**Test:** `ensureLoginSuccessMakesUserActive`

```java
@Test
void ensureLoginSuccessMakesUserActive() {
    store.add(ev("atcc1", "LOGIN_SUCCESS", 1));
    assertEquals(1, store.activeUsers().size());
    assertEquals("atcc1", store.activeUsers().get(0).username());
}
```

**Test:** `ensureLogoutRemovesActiveUser`

```java
@Test
void ensureLogoutRemovesActiveUser() {
    store.add(ev("atcc1", "LOGIN_SUCCESS", 1));
    store.add(ev("atcc1", "LOGOUT", 2));
    assertTrue(store.activeUsers().isEmpty());
    assertEquals(2, store.size()); // logout still in history
}
```

**Test:** `ensureConnectionLostRemovesActiveUser`

**Test:** `ensureFailedLoginIsRecordedButNotActive`

**Test:** `ensureClearResetsEventsAndActiveUsers`

---

### `LogFileWriterTest`

Location: `src/test/java/aisafe/app/loggingserver/store/LogFileWriterTest.java`

Unit test for `LogFileWriter` — verifies that an event is appended as a correct pipe-delimited line.

---

### `UdpLogReceiverIT`

Location: `src/test/java/aisafe/app/loggingserver/udp/UdpLogReceiverIT.java`

Implementation test that sends a real UDP datagram over loopback and verifies the event is stored. Exercises the full receive → parse → store pipeline.

---

## Coverage by Acceptance Criterion

- **AC091.1** (HTTP server embedded in logging server): verified by running `./run-us91.sh` and opening the browser
- **AC091.2** (two pages — last events + active users): `http://localhost:8080/events` and `http://localhost:8080/active` — verified manually
- **AC091.3** (pages kept updated): AJAX auto-refresh every 5 s — verified via browser DevTools Network tab
- **AC091.4** (AJAX without page reload): `fetch()` + `setInterval(5000)` in inline JavaScript — verified by browser DevTools
- **Store correctness**: `RemoteAccessLogStoreTest` (7 tests)
- **Parser robustness**: `LogEventParserTest` (7 tests)
- **File persistence**: `LogFileWriterTest`
- **End-to-end UDP receive**: `UdpLogReceiverIT`

---

## Acceptance Tests

**Prerequisites:** Run from `aisafe.base` directory.

---

**AC091.1 — HTTP server starts**

1. Run `./run-us91.sh`.
2. Expected output: `[US91] HTTP visualization server running on port 8080` and `[US90] Remote Accesses Logging Server listening on UDP 9090`.

---

**AC091.2 — Events page**

1. Open `http://localhost:8080/events` in a browser.
2. Expected: HTML page with a table of last recorded events (newest first).

---

**AC091.2 — Active users page**

1. Open `http://localhost:8080/active` in a browser.
2. Expected: HTML page with a table of currently active users.

---

**AC091.3 + AC091.4 — AJAX auto-refresh**

1. Open `http://localhost:8080/events`.
2. Open browser DevTools → Network tab.
3. Wait 5–10 seconds.
4. Expected: requests to `/api/events` appear every 5 seconds; the table updates without the page reloading.

---

**AC091.3 — Login event appears in both pages**

1. With the logging server running, authenticate as `pilot1` via `./run-pilot-client.sh`.
2. Expected: within 5 seconds, the `LOGIN_SUCCESS` event appears in `/events` and `pilot1` appears in `/active`.

---

**AC091.3 — Logout removes user from active page**

1. With `pilot1` logged in, select `0 - Exit` in the pilot client.
2. Expected: within 5 seconds, `pilot1` disappears from `/active`. The `LOGOUT` event still appears in `/events`.
