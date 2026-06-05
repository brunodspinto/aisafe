package aisafe.app.loggingserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Embedded HTTP server for US91 — Remote Accesses Logging Visualization.
 * Serves two HTML pages (last events, active users) with AJAX auto-refresh every 5 s.
 * Uses only the built-in com.sun.net.httpserver.HttpServer (no extra Maven dependencies).
 */
public final class LoggingHttpServer {

    static final int HTTP_PORT = 8080;

    private final AccessEventStore store;
    private HttpServer server;

    public LoggingHttpServer(final AccessEventStore store) {
        this.store = store;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.createContext("/",          ex -> handleRoot(ex));
        server.createContext("/events",    ex -> handleEventsPage(ex));
        server.createContext("/active",    ex -> handleActivePage(ex));
        server.createContext("/api/events", ex -> handleApiEvents(ex));
        server.createContext("/api/active", ex -> handleApiActive(ex));
        server.setExecutor(null);
        server.start();
    }

    // -------------------------------------------------------------------------
    // Root redirect
    // -------------------------------------------------------------------------

    private void handleRoot(final HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Location", "/events");
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    // -------------------------------------------------------------------------
    // HTML pages
    // -------------------------------------------------------------------------

    private void handleEventsPage(final HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1); ex.close(); return;
        }
        final String html = buildEventsHtml();
        sendHtml(ex, html);
    }

    private void handleActivePage(final HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1); ex.close(); return;
        }
        final String html = buildActiveHtml();
        sendHtml(ex, html);
    }

    // -------------------------------------------------------------------------
    // JSON API endpoints
    // -------------------------------------------------------------------------

    private void handleApiEvents(final HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1); ex.close(); return;
        }
        final List<AccessEvent> recent = store.getRecentEvents(100);
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < recent.size(); i++) {
            if (i > 0) json.append(",");
            json.append(toJson(recent.get(i)));
        }
        json.append("]");
        sendJson(ex, json.toString());
    }

    private void handleApiActive(final HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1); ex.close(); return;
        }
        final Map<String, AccessEvent> sessions = store.getActiveSessions();
        final StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (final AccessEvent ev : sessions.values()) {
            if (!first) json.append(",");
            json.append(toJson(ev));
            first = false;
        }
        json.append("]");
        sendJson(ex, json.toString());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String toJson(final AccessEvent ev) {
        return "{\"timestamp\":\"" + escapeJson(ev.getTimestamp()) + "\""
                + ",\"username\":\"" + escapeJson(ev.getUsername()) + "\""
                + ",\"clientIp\":\"" + escapeJson(ev.getClientIp()) + "\""
                + ",\"clientPort\":" + ev.getClientPort()
                + ",\"serviceId\":\"" + escapeJson(ev.getServiceId()) + "\""
                + ",\"eventType\":\"" + escapeJson(ev.getEventType()) + "\"}";
    }

    private static String escapeJson(final String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sendHtml(final HttpExchange ex, final String html) throws IOException {
        final byte[] body = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.getResponseHeaders().set("Connection", "close");
        ex.sendResponseHeaders(200, body.length);
        try (final OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static void sendJson(final HttpExchange ex, final String json) throws IOException {
        final byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Connection", "close");
        ex.sendResponseHeaders(200, body.length);
        try (final OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    // -------------------------------------------------------------------------
    // Inline HTML builders
    // -------------------------------------------------------------------------

    private static String buildEventsHtml() {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
                + "<title>Remote Access Events</title>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;margin:20px;background:#f5f5f5}"
                + "h1{color:#333}nav a{margin-right:16px;text-decoration:none;color:#0066cc}"
                + "table{border-collapse:collapse;width:100%;background:#fff}"
                + "th,td{border:1px solid #ccc;padding:8px 12px;text-align:left}"
                + "th{background:#0066cc;color:#fff}"
                + "tr:nth-child(even){background:#f0f0f0}"
                + "#status{margin-top:8px;font-size:.85em;color:#666}"
                + "</style></head><body>"
                + "<h1>Remote Access Events</h1>"
                + "<nav><a href='/events'>Last Events</a><a href='/active'>Active Users</a></nav>"
                + "<p id='status'>Loading...</p>"
                + "<table><thead><tr>"
                + "<th>Timestamp</th><th>Username</th><th>Client IP</th>"
                + "<th>Port</th><th>Service</th><th>Event</th>"
                + "</tr></thead><tbody id='tbody'></tbody></table>"
                + "<script>"
                + "function refresh(){"
                + "fetch('/api/events').then(r=>r.json()).then(data=>{"
                + "const tb=document.getElementById('tbody');"
                + "tb.innerHTML='';"
                + "const rows=[...data].reverse();"
                + "rows.forEach(e=>{"
                + "const tr=document.createElement('tr');"
                + "tr.innerHTML='<td>'+e.timestamp+'</td><td>'+e.username+'</td><td>'+e.clientIp+'</td>'"
                + "+'<td>'+e.clientPort+'</td><td>'+e.serviceId+'</td><td>'+e.eventType+'</td>';"
                + "tb.appendChild(tr);});"
                + "document.getElementById('status').textContent='Last update: '+new Date().toLocaleTimeString()+' — '+data.length+' event(s)';"
                + "}).catch(()=>document.getElementById('status').textContent='Error fetching data');"
                + "}"
                + "refresh();setInterval(refresh,5000);"
                + "</script></body></html>";
    }

    private static String buildActiveHtml() {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
                + "<title>Active Users</title>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;margin:20px;background:#f5f5f5}"
                + "h1{color:#333}nav a{margin-right:16px;text-decoration:none;color:#0066cc}"
                + "table{border-collapse:collapse;width:100%;background:#fff}"
                + "th,td{border:1px solid #ccc;padding:8px 12px;text-align:left}"
                + "th{background:#006633;color:#fff}"
                + "tr:nth-child(even){background:#f0f0f0}"
                + "#status{margin-top:8px;font-size:.85em;color:#666}"
                + "</style></head><body>"
                + "<h1>Currently Active Users</h1>"
                + "<nav><a href='/events'>Last Events</a><a href='/active'>Active Users</a></nav>"
                + "<p id='status'>Loading...</p>"
                + "<table><thead><tr>"
                + "<th>Username</th><th>Client IP</th><th>Port</th><th>Service</th><th>Login Time</th>"
                + "</tr></thead><tbody id='tbody'></tbody></table>"
                + "<script>"
                + "function refresh(){"
                + "fetch('/api/active').then(r=>r.json()).then(data=>{"
                + "const tb=document.getElementById('tbody');"
                + "tb.innerHTML='';"
                + "data.forEach(e=>{"
                + "const tr=document.createElement('tr');"
                + "tr.innerHTML='<td>'+e.username+'</td><td>'+e.clientIp+'</td><td>'+e.clientPort+'</td>'"
                + "+'<td>'+e.serviceId+'</td><td>'+e.timestamp+'</td>';"
                + "tb.appendChild(tr);});"
                + "document.getElementById('status').textContent='Last update: '+new Date().toLocaleTimeString()+' — '+data.length+' active user(s)';"
                + "}).catch(()=>document.getElementById('status').textContent='Error fetching data');"
                + "}"
                + "refresh();setInterval(refresh,5000);"
                + "</script></body></html>";
    }
}
