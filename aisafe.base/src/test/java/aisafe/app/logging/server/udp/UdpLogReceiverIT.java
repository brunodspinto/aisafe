package aisafe.app.logging.server.udp;

import aisafe.app.logging.server.store.RemoteAccessLogStore;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Implementation (integration) test for {@link UdpLogReceiver} (US090): a real UDP datagram
 * sent over the loopback interface is received, parsed and stored.
 */
class UdpLogReceiverIT {

    private static int freePort() throws Exception {
        try (DatagramSocket s = new DatagramSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Test
    void ensureDatagramIsReceivedParsedAndStored() throws Exception {
        final int port = freePort();
        final RemoteAccessLogStore store = new RemoteAccessLogStore();
        final UdpLogReceiver receiver = new UdpLogReceiver(port, store, null);

        final Thread serverThread = new Thread(receiver);
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300); // allow the socket to bind

        final String payload = "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS";
        try (DatagramSocket client = new DatagramSocket()) {
            final byte[] data = payload.getBytes(StandardCharsets.US_ASCII);
            client.send(new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), port));
        }

        final long deadline = System.currentTimeMillis() + 2000;
        while (store.size() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        receiver.stop();

        assertEquals(1, store.size());
        assertEquals("atcc1", store.recent(1).get(0).username());
        assertEquals(1, store.activeUsers().size());
    }
}
