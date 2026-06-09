package aisafe.app.weatherperson;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP client for the Weather Person remote application (US044).
 * Encapsulates protocol communication with the AISafe TCP server (Facade over the socket).
 *
 * <p>It has no dependency on any JPA or repository class — persistence is performed
 * exclusively server-side (AC044.2).
 */
public final class WeatherPersonTcpClient implements Closeable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public WeatherPersonTcpClient(final String host, final int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Sends LOGIN (declaring the {@code WEATHER} service) and reads the server response.
     *
     * @return {@code true} if the server responded {@code OK}
     */
    public boolean login(final String username, final String password) throws IOException {
        out.println("LOGIN " + username + " " + password + " WEATHER");
        return "OK".equals(in.readLine());
    }

    /**
     * Sends REGISTER_WEATHER and returns the single-line server response (US041).
     *
     * @return the server response (e.g. {@code OK <id>} or {@code ERROR ...})
     */
    public String registerWeather(final String areaCode, final String provider,
                                  final String format, final String dateTime,
                                  final String temperature, final String windSpeed,
                                  final String windDir, final String pressure,
                                  final String visibility) throws IOException {
        out.println("REGISTER_WEATHER " + areaCode + " " + provider + " " + format
                + " " + dateTime + " " + temperature + " " + windSpeed
                + " " + windDir + " " + pressure + " " + visibility);
        return in.readLine();
    }

    /**
     * Reads a local CSV file and sends it via IMPORT_BULK (US042).
     *
     * @param filePath path to the local CSV file
     * @return the server response (e.g. {@code OK saved=N failures=M} or {@code ERROR ...})
     */
    public String importBulk(final String filePath) throws IOException {
        final String csvContent = Files.readString(Path.of(filePath));
        out.println("IMPORT_BULK " + csvContent.length());
        final String ready = in.readLine();
        if (!"READY".equals(ready)) {
            return "ERROR unexpected server response: " + ready;
        }
        out.print(csvContent);
        out.flush();
        return in.readLine();
    }

    /**
     * Sends CONSULT_WEATHER and reads the multi-line response (US043).
     *
     * @param date     ISO-8601 date (e.g. {@code 2026-06-01})
     * @param areaCode the air control area code
     * @return list of weather record strings, empty if none found
     */
    public List<String> consultWeather(final String date, final String areaCode) throws IOException {
        out.println("CONSULT_WEATHER " + date + " " + areaCode);
        final List<String> results = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            results.add(line);
        }
        return results;
    }

    /**
     * Sends LIST_AREAS and reads the multi-line response.
     *
     * @return list of active air control area codes
     */
    public List<String> listAreas() throws IOException {
        out.println("LIST_AREAS");
        final List<String> areas = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            areas.add(line);
        }
        return areas;
    }

    /**
     * Sends EXIT and reads the BYE response.
     */
    public void exit() throws IOException {
        out.println("EXIT");
        in.readLine();
    }

    /** @return the local address of the TCP socket (used by the UDP access logger). */
    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    /** @return the local port of the TCP socket (used by the UDP access logger). */
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
