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

    public void nachrichtSenden(String text) throws IOException {
        // zum Debuggen / anschauen ein System.out
        System.out.println("UDP sende an " + zielAdresse + ":" + zielPort);
        
        String nachricht = "MSG|" + text;
        byte[] daten = nachricht.getBytes();
        DatagramPacket paket = new DatagramPacket(daten, daten.length, zielAdresse, zielPort);
        socket.send(paket);
    }

    public void lauschen(ChatEvents events) {        
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