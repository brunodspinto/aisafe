package aisafe.app.logging.server.model;

import java.time.LocalDateTime;

/**
 * A remote user currently considered active — i.e. whose most recent event is a
 * {@code LOGIN_SUCCESS} not yet followed by a {@code LOGOUT} or {@code CONNECTION_LOST}.
 *
 * <p>This is the view consumed by the US091 "currently active users" page.
 *
 * @param username   the active user's username
 * @param clientIp   the client's TCP IP address
 * @param clientPort the client's TCP port
 * @param service    the service the user is connected through ({@code US44}, {@code US78}, {@code US86})
 * @param since      the timestamp of the login that started the active session
 */
public record ActiveUser(
        String username,
        String clientIp,
        int clientPort,
        String service,
        LocalDateTime since) {
}
