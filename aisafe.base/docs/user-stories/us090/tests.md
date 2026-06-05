# US090 — Tests and Coverage

## Scope

US090 is a UDP server plus an in-memory/file store. The automated tests focus on the **parsing**, the **store logic** (ordering + active-users state machine) and the **UDP reception** over a real socket. Business rules belong to the client USs, not here.

---

## Automated Tests

### `LogEventParserTest` — `src/test/java/aisafe/app/loggingserver/udp/`

In-memory unit tests of the payload parser.

| Test | Verifies |
|------|----------|
| `ensureValidPayloadIsParsedIntoAllSixFields` | A well-formed payload maps to the six fields (AC090.3) |
| `ensureNullOrBlankPayloadIsRejected` | `null`/blank → empty |
| `ensureWrongNumberOfFieldsIsRejected` | 3 or 7 fields → empty |
| `ensureEmptyMandatoryFieldsAreRejected` | Empty username/service/event → empty |
| `ensureBadTimestampFallsBackToNowButStillParses` | Lenient timestamp, event not dropped |
| `ensureBadPortFallsBackToMinusOne` | Lenient port |
| `ensureServiceIdentifierIsCarriedThroughForEachClient` | `US44`/`US86` preserved (AC090.3) |

### `RemoteAccessLogStoreTest` — `src/test/java/aisafe/app/loggingserver/store/`

Unit tests of ordering and the active-users state machine.

| Test | Verifies |
|------|----------|
| `ensureRecentReturnsNewestFirst` | Newest event first (for US091) |
| `ensureRecentRespectsTheLimit` | `recent(n)` honours the limit |
| `ensureLoginSuccessMakesUserActive` | LOGIN_SUCCESS → active (AC090.2) |
| `ensureLogoutRemovesActiveUser` | LOGOUT removes the session |
| `ensureConnectionLostRemovesActiveUser` | CONNECTION_LOST removes the session |
| `ensureFailedLoginIsRecordedButNotActive` | LOGIN_FAILED recorded, not active |
| `ensureClearResetsEventsAndActiveUsers` | `clear()` resets state |

### `LogFileWriterTest` — `src/test/java/aisafe/app/loggingserver/store/`

| Test | Verifies |
|------|----------|
| `ensureEventIsAppendedAsPipeDelimitedLine` | Event persisted as one pipe-delimited line (AC090.4), using `@TempDir` |

### `UdpLogReceiverIT` — `src/test/java/aisafe/app/loggingserver/udp/`

| Test | Verifies |
|------|----------|
| `ensureDatagramIsReceivedParsedAndStored` | A real UDP datagram over loopback is received, parsed and stored (AC090.1) |

**Result:** 16 tests, all passing.

---

## Coverage by Acceptance Criterion

- **AC090.1** (UDP server receives events): `UdpLogReceiverIT` + manual.
- **AC090.2** (four event types recorded): `RemoteAccessLogStoreTest` (login/logout/disconnect/failed).
- **AC090.3** (timestamp, username, IP, port, service): `LogEventParserTest`.
- **AC090.4** (structured storage, not just print): `RemoteAccessLogStoreTest` + `LogFileWriterTest`.

---

## Manual Acceptance Test (end-to-end)

**Prerequisites:** none for US090 (no database). For the full flow also start the H2 DB (port 9093), the AISafe main app (`AiSafeConsoleApp`, TCP 9999) and a client.

1. Start `aisafe.app.loggingserver.RemoteAccessLoggingServerApp` → `listening on UDP 9090` and HTTP on 8080.
2. Run the US078 client (`CollaboratorTcpClientApp`), log in as `atcc1 / Password1`.
    - Expected: server console shows `... | atcc1 | ... | US78 | LOGIN_SUCCESS`.
3. Choose Exit → `... | US78 | LOGOUT`.
4. Log in again with a wrong password → `... | US78 | LOGIN_FAILED`.
5. Run the US086 client (`PilotTcpClientApp`), log in as `pilot1 / Password1` → `... | US86 | LOGIN_SUCCESS`.
6. Open `logs.txt` → one pipe-delimited line per event; or open `http://localhost:8080/` to see them on the US091 pages.

**Isolated US090 test (no TCP/login):** with the server running, send a datagram from PowerShell:

```powershell
$u=New-Object System.Net.Sockets.UdpClient; $m="2026-06-04 18:00:00 | teste | 127.0.0.1 | 5000 | US78 | LOGIN_SUCCESS"; $b=[Text.Encoding]::ASCII.GetBytes($m); $u.Send($b,$b.Length,"127.0.0.1",9090); $u.Close()
```