package chat.server;
import java.io.*;
import java.net.Socket;

class ClientHandler extends Thread {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final UserDatabase userDb;
    private final OnlineUsers online;
    private String username;

    public ClientHandler(Socket socket, UserDatabase userDb, OnlineUsers online) throws IOException {
        this.socket = socket;
        this.userDb = userDb;
        this.online = online;

        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                process(line);
            }
        } catch (IOException e) {
            System.out.println("Client getrennt: " + username);
        } finally {
            if (username != null) {
                online.remove(username);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
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
            default:
                out.println("ERROR|Unknown command");
        }
    }

    private void handleRegister(String[] p) {
        if (p.length < 3) {
            out.println("REGISTER_FAIL|Invalid format");
            return;
        }

        String user = p[1];
        String pass = p[2];

        if (userDb.register(user, pass)) {
            out.println("REGISTER_OK");
        } else {
            out.println("REGISTER_FAIL|Username existiert bereits");
        }
    }

    private void handleLogin(String[] p) {
        if (p.length < 3) {
            out.println("LOGIN_FAIL|Invalid format");
            return;
        }

        String user = p[1];
        String pass = p[2];

        if (!userDb.login(user, pass)) {
            out.println("LOGIN_FAIL|Falsche Login-Daten");
            return;
        }

        username = user;
        online.add(username, this);
        out.println("LOGIN_OK");
    }

    private void sendUserList() {
        String users = String.join(",", online.getUsernames());
        out.println("USERS|" + users);
    }

    private void handleInvite(String[] p) {
        if (p.length < 3) return;

        String targetUser = p[1];
        String udpPort = p[2];

        ClientHandler target = online.getHandler(targetUser);
        if (target == null) return;

        String ip = socket.getInetAddress().getHostAddress();

        target.out.println(
            "INVITE_FROM|" + username + "|" + ip + "|" + udpPort
        );
    }
}
