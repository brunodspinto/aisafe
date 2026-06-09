package aisafe.dsl;

import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.LegAst;
import aisafe.dsl.ast.SegmentAst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Serialises a {@link FlightPlanAst} to a temporary JSON file in the format expected by
 * the {@code flight_tester} C binary.
 *
 * <p>Output format (one-element array matching {@code simulation/flight_plans.json}):</p>
 * <pre>
 * [
 *   {
 *     "identifier": "TP85",
 *     "flight_type": "REGULAR",
 *     "legs": [
 *       {
 *         "segments": [
 *           {
 *             "start_coord": [41.15, -8.61],
 *             "end_coord":   [38.72, -9.14],
 *             "altitude_m":  10000.0,
 *             "mode":        "cruise"
 *           }
 *         ]
 *       }
 *     ]
 *   }
 * ]
 * </pre>
 *
 * <p>The caller is responsible for deleting the returned temp file after use.</p>
 */
public class FlightPlanJsonSerializer {

    /**
     * Serialises the given AST to a UTF-8 temporary JSON file.
     *
     * @param ast the parsed flight plan AST (non-null)
     * @return path to the temporary file; the caller must delete it when done
     * @throws IOException if the temp file cannot be written
     */
    public Path toTempFile(final FlightPlanAst ast) throws IOException {
        Objects.requireNonNull(ast, "ast must not be null");
        final String json = buildJson(ast);
        final Path tmp = Files.createTempFile("aisafe-fp-", ".json");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        return tmp;
    }

    private String buildJson(final FlightPlanAst ast) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[\n  {\n");
        sb.append("    \"identifier\": \"").append(jsonEscape(ast.identifier())).append("\",\n");
        sb.append("    \"flight_type\": \"").append(jsonEscape(ast.flightType().name())).append("\",\n");
        sb.append("    \"legs\": [\n");

        final List<LegAst> legs = ast.legs();
        for (int li = 0; li < legs.size(); li++) {
            sb.append("      {\n");
            sb.append("        \"segments\": [\n");

            final List<SegmentAst> segments = legs.get(li).segments();
            for (int si = 0; si < segments.size(); si++) {
                final SegmentAst seg = segments.get(si);
                sb.append("          {\n");
                sb.append("            \"start_coord\": [")
                  .append(String.format(Locale.US, "%.6f", seg.from().latitude())).append(", ")
                  .append(String.format(Locale.US, "%.6f", seg.from().longitude())).append("],\n");
                sb.append("            \"end_coord\": [")
                  .append(String.format(Locale.US, "%.6f", seg.to().latitude())).append(", ")
                  .append(String.format(Locale.US, "%.6f", seg.to().longitude())).append("],\n");
                sb.append("            \"altitude_m\": ")
                  .append(String.format(Locale.US, "%.6f", seg.altitudeMeters())).append(",\n");
                sb.append("            \"mode\": \"cruise\"\n");
                sb.append("          }");
                if (si < segments.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }

            sb.append("        ]\n      }");
            if (li < legs.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("    ]\n  }\n]");
        return sb.toString();
    }

    private static String jsonEscape(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
