package chat.client;
import java.io.*;
import java.net.*;

public class UDPChat {
    private DatagramSocket socket;
    private int zielPort;
    private InetAddress zielAdresse;

    public UDPChat(int lauschPort) throws SocketException {
        socket = new DatagramSocket(lauschPort);
    }

    public void zielFestlegen(InetAddress host, int port) {
        this.zielAdresse = host;
        this.zielPort = port;
    }

    // sendet Nachtricht von ChatGUI.java/nachrichtenSenden()
    public void nachrichtSenden(String text) throws IOException {
        if (zielAdresse == null) {
            // Nachricht zum Debuggen
            System.out.println("DEBUG-UDP-FAIL: Senden nicht möglich, ZielAdresse ist NULL!");
            return;
        }

        // Nachricht zum Debuggen
        System.out.println("DEBUG-UDP-SEND: Sende '" + text + "' an " + zielAdresse + ":" + zielPort);

        String protokollNachricht;
        // Technische Befehle ohne MSG-Präfix senden
        if (text.equals("HELLO") || text.equals("HELLO_ACK")) {
            protokollNachricht = text;
        } else {
            // Alles andere ist eine Chat-Nachricht
            protokollNachricht = "MSG|" + text;
        }

        byte[] daten = protokollNachricht.getBytes();
        DatagramPacket paket = new DatagramPacket(daten, daten.length, zielAdresse, zielPort);
        socket.send(paket);
    }

    // wartet auf Nachrichten des Partners
    public void listen(ChatEvents events) {        
        new Thread(() -> {
            try {
                byte[] puffer = new byte[1024];
                while (true) {
                    DatagramPacket empfangenesPak = new DatagramPacket(puffer, puffer.length);
                    socket.receive(empfangenesPak);

                    // zum Debuggen / anschauen ein System.out
                    System.out.println("UDP empfangen von " + empfangenesPak.getAddress() + ":" + empfangenesPak.getPort());

                    String nachricht = new String(empfangenesPak.getData(), 0, empfangenesPak.getLength());
                    events.beiUdpNachricht(nachricht);
                }
            } catch (Exception ignored) {}
        }).start();
    }
}