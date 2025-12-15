package client;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private int udpPort;

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean register(String username, String password) throws IOException {
        out.println("REGISTER|" + username + "|" + password);
        String response = in.readLine();
        return response.equals("REGISTER_OK");
    }

    public boolean login(String username, String password) throws IOException {
        out.println("LOGIN|" + username + "|" + password);
        String response = in.readLine();

        if (response.equals("LOGIN_OK")) {
            this.username = username;
            this.udpPort = 6000 + new Random().nextInt(1000);
            return true;
        }
        return false;
    }

    public void requestUserList() {
        out.println("GET_USERS");
    }

    public void sendInvite(String targetUser) {
        out.println("INVITE|" + targetUser + "|" + udpPort);
    }

    public void listenAsync(ChatEvents events) {
        new Thread(() -> {
            try {
                String line;

                while ((line = in.readLine()) != null) {
                    process(line, events);
                }
            } catch (IOException e) {
                events.onDisconnect();
            }
        }).start();
    }

    private void process(String msg, ChatEvents events) {
        System.out.println("RECV: " + msg);
        String[] parts = msg.split("\\|");
        String cmd = parts[0];

        switch (cmd) {
            case "USERS":
            if (parts.length > 1) {
                List<String> list = Arrays.asList(parts[1].split(","));
                events.onUserList(list);
            }
            break;

            case "INVITE":
            String from = parts[1];
            events.onInvite(from);
            break;
        }
    }

    public String getUsername() {
        return username;
    }

    public int getUdpPort() {
        return udpPort;
    }
}