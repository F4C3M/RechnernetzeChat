package server;
import java.io.*;
import java.net.*;

class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserDatabase userDb;
    private OnlineUsers online;
    private String username = null;

    public ClientHandler(Socket s, UserDatabase db, OnlineUsers o) {
        this.socket = s;
        this.userDb = db;
        this.online = o;
        
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(s.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String line;

            while ((line = in.readLine()) != null) {
                process(line);
            }
        } catch (IOException e) {
            System.out.println("Client getrennt: " + username);
        } finally {
            if (username != null) online.remove(username);
        }
    }

    private void process(String msg) {
        System.out.println("RECV: " + msg);
        String[] parts = msg.split("\\|");
        String cmd = parts[0];

        switch (cmd) {
            case "REGISTER":
                handleRegister(parts);
                break;
            case "LOGIN":
                handleLogin(parts);
                break;
            case "GET_USERS":
                sendUserList();
                break;
            case "INVITE":
                handleInvite(parts);
                break;
        }
    }

    private void handleRegister(String[] p) {
        if (p.length < 3) return;
        String u = p[1], pw = p[2];
        if (userDb.register(u, pw)) out.println("REGISTER_OK");
        else out.println("REGISTER_FAIL|Username existiert bereits");
    }

    private void handleLogin(String[] p) {
        if (p.length < 3) return;
        String u = p[1], pw = p[2];

        if (userDb.login(u, pw)) {
            username = u;
            online.add(u, this);
            out.println("LOGIN_OK");
        } else out.println("LOGIN_FAIL|Falsche Daten");
    }

    private void sendUserList() {
        String users = String.join(",", online.getUsernames());
        out.println("USERS|" + users);
    }

    private void handleInvite(String[] p) {
        if (p.length < 3) return;

        String target = p[1];
        String udpPort = p[2];

        ClientHandler h = online.getHandler(target);
        if (h != null) {
            String ip = socket.getInetAddress().getHostAddress();
            h.out.println("INVITE_FROM|" + username + "|" + ip + "|" + udpPort);
        }
    }
}