package chat.client;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private int udpPort;

    private final Object lock = new Object();
    private String lastResponse;

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

    public boolean register(String username, String password) {
        synchronized (lock) {
            out.println("REGISTER|" + username + "|" + password);
            lastResponse = null;

            while (lastResponse == null) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {}
            }

            return lastResponse.equals("REGISTER_OK");
        }
    }

    public boolean login(String username, String password) throws IOException {
        synchronized (lock) {
            out.println("LOGIN|" + username + "|" + password);
            lastResponse = null;

            while (lastResponse == null) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {}
            }

            if (lastResponse.equals("LOGIN_OK")) {
                this.username = username;
                this.udpPort = 6000 + new Random().nextInt(1000);
                return true;
            }
            return false;
        }
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
                    handleServerMessage(line, events);
                }
            } catch (IOException e) {
                events.onDisconnect();
            }
        }).start();
    }

    private void handleServerMessage(String msg, ChatEvents events) {
        System.out.println("RECV: " + msg);

        if (msg.startsWith("LOGIN_") || msg.startsWith("REGISTER_")) {
            synchronized (lock) {
                lastResponse = msg;
                lock.notifyAll();
            }
            return;
        }

        String[] parts = msg.split("\\|");
        switch (parts[0]) {
            case "USERS":
                events.onUserList(Arrays.asList(parts[1].split(",")));
                break;

            case "INVITE_FROM":
                events.onInvite(parts[1] + "|" + parts[2] + "|" + parts[3]);
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