package aisafe.app.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link RemoteAccessLogger} (US044 / US078 / US086 → US090).
 * Binds a UDP receiver on a loopback port and asserts the pipe-delimited payload format.
 */
class RemoteAccessLoggerTest {

    private DatagramSocket receiver;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        receiver = new DatagramSocket(0);
        receiver.setSoTimeout(2000);
        port = receiver.getLocalPort();
    }

    @AfterEach
    void tearDown() {
        receiver.close();
    }

    private String receiveOnePacket() throws Exception {
        final byte[] buffer = new byte[512];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiver.receive(packet);
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);
    }

    @Test
    void ensureLoginSuccessDatagramHasExpectedFormat() throws Exception {
        final RemoteAccessLogger logger = new RemoteAccessLogger("127.0.0.1", port, "US78");
        logger.log("atcc1", "127.0.0.1", 50231, "LOGIN_SUCCESS");

        final String[] fields = receiveOnePacket().split("\\|");
        assertEquals(6, fields.length);
        assertEquals("atcc1", fields[1].trim());
        assertEquals("127.0.0.1", fields[2].trim());
        assertEquals("50231", fields[3].trim());
        assertEquals("US78", fields[4].trim());
        assertEquals("LOGIN_SUCCESS", fields[5].trim());
    }

    @Test
    void ensureEventNameIsCarriedInPayload() throws Exception {
        final RemoteAccessLogger logger = new RemoteAccessLogger("127.0.0.1", port, "US78");
        logger.log("atcc2", "10.0.0.5", 40000, "CONNECTION_LOST");

        assertEquals("CONNECTION_LOST", receiveOnePacket().split("\\|")[5].trim());
    }

    @Test
    void ensureServiceIdIsCarriedInPayload() throws Exception {
        final RemoteAccessLogger logger = new RemoteAccessLogger("127.0.0.1", port, "US86");
        logger.log("pilot1", "10.0.0.9", 41000, "LOGIN_SUCCESS");

        assertEquals("US86", receiveOnePacket().split("\\|")[4].trim());
    }
}
