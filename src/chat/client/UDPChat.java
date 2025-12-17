package chat.client;
import java.io.*;
import java.net.*;

public class UDPChat {
    private DatagramSocket socket;
    private int remotePort;
    private InetAddress remoteHost;

    public UDPChat(int listenPort) throws SocketException {
        socket = new DatagramSocket(listenPort);
    }

    public void setTarget(InetAddress host, int port) {
        this.remoteHost = host;
        this.remotePort = port;
    }

    public void sendMessage(String text) throws IOException {
        // zum Debuggen / anschauen ein System.out
        System.out.println("UDP sende an " + remoteHost + ":" + remotePort);
        
        String msg = "MSG|" + text;
        byte[] data = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteHost, remotePort);
        socket.send(packet);
    }

    public void listen(ChatEvents events) {        
        new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);

                    // zum Debuggen / anschauen ein System.out
                    System.out.println("UDP empfangen von " + p.getAddress() + ":" + p.getPort());

                    String msg = new String(p.getData(), 0, p.getLength());
                    events.onUdpMessage(msg);
                }
            } catch (Exception ignored) {}
        }).start();
    }
}