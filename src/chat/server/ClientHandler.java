package chat.server;
import java.io.*;
import java.net.Socket;

class ClientHandler extends Thread {
    private final Socket socket;
    private final BufferedReader eingabe;
    private final PrintWriter ausgabe;
    private final UserDatabase userDatenbank;
    private final OnlineUsers onlineUser;
    private String username;

    public ClientHandler(Socket socket, UserDatabase userDatenbank, OnlineUsers onlineUser) throws IOException {
        this.socket = socket;
        this.userDatenbank = userDatenbank;
        this.onlineUser = onlineUser;

        this.eingabe = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.ausgabe = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String eingehendeNachricht;
            while ((eingehendeNachricht = eingabe.readLine()) != null) {
                nachrichtenVerarbeiten(eingehendeNachricht);
            }
        } catch (IOException e) {
            System.out.println("Client getrennt: " + username);
        } finally {
            if (username != null) {
                onlineUser.entfernen(username);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    // ÄNDERUNG: case: INVITE (verändert) || case: INVITE_ACCEPT (neu)
    private void nachrichtenVerarbeiten(String nachricht) {
        System.out.println("RECV: " + nachricht);

        String[] nachrichtenTeile = nachricht.split("\\|");
        String befehl = nachrichtenTeile[0];

        switch (befehl) {
            case "REGISTER":
                registrierungBearbeiten(nachrichtenTeile);
                break;
            case "LOGIN":
                anmeldungBearbeiten(nachrichtenTeile);
                break;
            case "GET_USERS":
                userlisteSenden();
                break;
            case "INVITE":
                einladungBearbeiten(nachrichtenTeile);
                break;
            case "INVITE_ACCEPT":
                inviteAcceptBearbeiten(nachrichtenTeile);
                break;
            default:
                ausgabe.println("ERROR|Unknown command");
        }
    }

    private void registrierungBearbeiten(String[] nachrichtenTeile) {
        if (nachrichtenTeile.length < 3) {
            ausgabe.println("REGISTER_FAIL|Invalid format");
            return;
        }

        String benutzername = nachrichtenTeile[1];
        String passwort = nachrichtenTeile[2];

        if (userDatenbank.registrieren(benutzername, passwort)) {
            ausgabe.println("REGISTER_OK");
        } else {
            ausgabe.println("REGISTER_FAIL|Username existiert bereits");
        }
    }

    private void anmeldungBearbeiten(String[] p) {
        if (p.length < 3) {
            ausgabe.println("LOGIN_FAIL|Invalid format");
            return;
        }

        String benutzername = p[1];
        String passwort = p[2];

        if (!userDatenbank.anmelden(benutzername, passwort)) {
            ausgabe.println("LOGIN_FAIL|Falsche Login-Daten");
            return;
        }

        username = benutzername;
        onlineUser.hinzufuegen(username, this);
        ausgabe.println("LOGIN_OK");
    }

    private void userlisteSenden() {
        String users = String.join(",", onlineUser.getUsernames());
        ausgabe.println("USERS|" + users);
    }


    // VERÄNDERUNG: für Invite in nur eine Richtung reicht tut sein
    private void einladungBearbeiten(String[] p) {
        if (p.length < 3) return;

        String zielUser = p[1];
        int udpPort = Integer.parseInt(p[2]);

        ClientHandler zielHandler = onlineUser.getUserHandler(zielUser);
        if (zielHandler == null) return;

        String ipAdresse = socket.getInetAddress().getHostAddress();

        // NUR an User2
        zielHandler.ausgabe.println("INVITE_FROM|" + username + "|" + ipAdresse + "|" + udpPort);
    }

    // VERÄNDERUNG: neue Methode (für Einladungen)
    private void inviteAcceptBearbeiten(String[] p) {
        if (p.length < 3) return;

        String einladenderUser = p[1]; // derjenige, der eingeladen hat
        int udpPort = Integer.parseInt(p[2]); // UDP-Port von User2

        ClientHandler einladenderHandler = onlineUser.getUserHandler(einladenderUser);
        if (einladenderHandler != null) {
            // IP von User2
            String ipAdresse = socket.getInetAddress().getHostAddress();
            // User1 bekommt Nachricht, dass User2 angenommen hat
            einladenderHandler.ausgabe.println(
                "INVITE_ACCEPT_FROM|" + username + "|" + ipAdresse + "|" + udpPort
            );
        }
    }
}