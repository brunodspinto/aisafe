package aisafe.tcpserver.weatherperson;

import aisafe.weatherdata.application.ConsultWeatherDataController;
import aisafe.weatherdata.application.ImportBulkWeatherDataController;
import aisafe.weatherdata.application.RegisterWeatherDataController;
import aisafe.weatherdata.domain.WeatherData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Handles the command loop for an authenticated Weather Person TCP session (US044).
 *
 * <p>Supported commands:
 * <pre>
 *   REGISTER_WEATHER &lt;areaCode&gt; &lt;provider&gt; &lt;format&gt; &lt;dateTime&gt; &lt;temp&gt; &lt;windSpeed&gt; &lt;windDir&gt; &lt;pressure&gt; &lt;visibility&gt;
 *   IMPORT_BULK &lt;charLength&gt;
 *   CONSULT_WEATHER &lt;date&gt; &lt;areaCode&gt;
 *   LIST_AREAS
 *   EXIT
 * </pre>
 */
public final class WeatherPersonSessionHandler {

    private final BufferedReader in;
    private final PrintWriter out;

    public WeatherPersonSessionHandler(final BufferedReader in, final PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Reads and dispatches Weather Person commands until EXIT or the connection closes.
     */
    public void handle() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            final String cmd = line.trim();
            if (cmd.startsWith("REGISTER_WEATHER")) {
                handleRegisterWeather(cmd);
            } else if (cmd.startsWith("IMPORT_BULK")) {
                handleImportBulk(cmd);
            } else if (cmd.startsWith("CONSULT_WEATHER")) {
                handleConsultWeather(cmd);
            } else if (cmd.equals("LIST_AREAS")) {
                handleListAreas();
            } else if (cmd.equals("EXIT")) {
                out.println("BYE");
                return;
            } else {
                out.println("UNKNOWN_COMMAND");
            }
        }
    }

    // -------------------------------------------------------------------------
    // US041 – Register weather data
    // -------------------------------------------------------------------------

    /**
     * Command:
     * REGISTER_WEATHER &lt;areaCode&gt; &lt;provider&gt; &lt;format&gt; &lt;dateTime(ISO)&gt;
     *                  &lt;temp&gt; &lt;windSpeed&gt; &lt;windDir&gt; &lt;pressure&gt; &lt;visibility&gt;
     *
     * Example:
     * REGISTER_WEATHER LPPC MeteoGroup CSV 2026-06-01T12:00:00 15.2 30.5 NW 1013.2 9999
     */
    private void handleRegisterWeather(final String cmd) {
        // 10 tokens: command + 9 parameters
        final String[] p = cmd.split(" ", 10);
        if (p.length < 10) {
            out.println("ERROR usage: REGISTER_WEATHER <areaCode> <provider> <format> "
                    + "<dateTime(ISO)> <temp> <windSpeed> <windDir> <pressure> <visibility>");
            return;
        }
        try {
            final String areaCode        = p[1];
            final String provider        = p[2];
            final String format          = p[3];
            final LocalDateTime dateTime = LocalDateTime.parse(p[4]);
            final double temperature     = Double.parseDouble(p[5]);
            final double windSpeed       = Double.parseDouble(p[6]);
            final String windDir         = p[7];
            final double pressure        = Double.parseDouble(p[8]);
            final double visibility      = Double.parseDouble(p[9]);

            final var saved = new RegisterWeatherDataController().registerWeatherData(
                    areaCode, provider, format, dateTime,
                    temperature, windSpeed, windDir, pressure, visibility);

            final String id = saved.identity() != null ? saved.identity().toString() : "saved";
            out.println("OK " + id);

        } catch (final DateTimeParseException e) {
            out.println("ERROR invalid dateTime – use ISO-8601 (e.g. 2026-06-01T12:00:00)");
        } catch (final NumberFormatException e) {
            out.println("ERROR numeric parameter is invalid: " + e.getMessage());
        } catch (final Exception e) {
            out.println("ERROR " + sanitize(e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // US042 – Import bulk weather data
    // -------------------------------------------------------------------------

    /**
     * Protocol:
     * 1. Client:  IMPORT_BULK &lt;charLength&gt;
     * 2. Server:  READY
     * 3. Client:  (streams exactly charLength characters of CSV)
     * 4. Server:  OK saved=N failures=M [| [msg] ...]
     */
    private void handleImportBulk(final String cmd) throws IOException {
        final String[] p = cmd.split(" ", 2);
        if (p.length < 2) {
            out.println("ERROR usage: IMPORT_BULK <charLength>");
            return;
        }

        final int charLength;
        try {
            charLength = Integer.parseInt(p[1].trim());
            if (charLength <= 0) throw new NumberFormatException();
        } catch (final NumberFormatException e) {
            out.println("ERROR invalid char length");
            return;
        }

        out.println("READY");

        // Read exactly charLength characters sent by the client
        final char[] buffer = new char[charLength];
        int read = 0;
        while (read < charLength) {
            final int n = in.read(buffer, read, charLength - read);
            if (n == -1) {
                out.println("ERROR connection closed while reading CSV content");
                return;
            }
            read += n;
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("aisafe-weather-bulk-", ".csv");
            Files.writeString(tempFile, new String(buffer));

            final var result = new ImportBulkWeatherDataController().importWeatherData(tempFile.toString());

            final StringBuilder reply = new StringBuilder("OK saved=")
                    .append(result.saved())
                    .append(" failures=")
                    .append(result.failures().size());

            if (!result.failures().isEmpty()) {
                reply.append(" |");
                for (final String f : result.failures()) {
                    reply.append(" [").append(sanitize(f)).append("]");
                }
            }
            out.println(reply);

        } catch (final Exception e) {
            out.println("ERROR " + sanitize(e.getMessage()));
        } finally {
            if (tempFile != null) Files.deleteIfExists(tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // US043 – Consult weather data
    // -------------------------------------------------------------------------

    /**
     * Command:
     * CONSULT_WEATHER &lt;date(ISO)&gt; &lt;areaCode&gt;
     *
     * Example: CONSULT_WEATHER 2026-06-01 LPPC
     *
     * Response: one line per record, terminated by an empty line.
     */
    private void handleConsultWeather(final String cmd) {
        final String[] p = cmd.split(" ", 3);
        if (p.length < 3) {
            out.println("ERROR usage: CONSULT_WEATHER <date(ISO)> <areaCode>");
            out.println(); // end-of-data marker
            return;
        }
        try {
            final LocalDate date  = LocalDate.parse(p[1]);
            final String areaCode = p[2];

            final Iterable<WeatherData> results =
                    new ConsultWeatherDataController().consultWeatherData(date, areaCode);

            for (final WeatherData wd : results) {
                out.println(wd.toString());
            }
            out.println(); // empty line signals end of data

        } catch (final DateTimeParseException e) {
            out.println("ERROR invalid date – use ISO-8601 (e.g. 2026-06-01)");
            out.println();
        } catch (final Exception e) {
            out.println("ERROR " + sanitize(e.getMessage()));
            out.println();
        }
    }

    // -------------------------------------------------------------------------
    // List Air Control Areas (helper)
    // -------------------------------------------------------------------------

    private void handleListAreas() {
        try {
            for (final var area : new ConsultWeatherDataController().activeAirControlAreas()) {
                out.println(area.identity());
            }
            out.println(); // end-of-data marker
        } catch (final Exception e) {
            out.println("ERROR " + sanitize(e.getMessage()));
            out.println();
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static String sanitize(final String msg) {
        return msg == null ? "unknown error" : msg.replace('\n', ' ').replace('\r', ' ');
    }
}
