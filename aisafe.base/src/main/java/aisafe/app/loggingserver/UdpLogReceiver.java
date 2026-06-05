package aisafe.app.loggingserver;

/**
 * US90: Receives remote-access events via UDP and stores them in AccessEventStore.
 * TODO US90: implement UDP DatagramSocket receiver here.
 */
public final class UdpLogReceiver {

    private final AccessEventStore store;

    public UdpLogReceiver(final AccessEventStore store) {
        this.store = store;
    }

    // TODO US90: open DatagramSocket on the agreed port, loop on receive(),
    //            parse each packet with AccessEvent.parse(), call store.addEvent().
}
