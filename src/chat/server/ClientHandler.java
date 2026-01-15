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

    @Override // nimmt eingehende Nachrichten an von Client
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

    // verarbeitet die Nachrichten, welche vom Client [ChatClient.java] kommen
    private void nachrichtenVerarbeiten(String nachricht) {
        System.out.println("RECV: " + nachricht);

        // splitet die Nachricht vom Client [ChatClient.java] und schaut durch
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

    // verarbeitet die Registrierung des Clients [ChatClient.java]
    private void registrierungBearbeiten(String[] nachrichtenTeile) {
        // Nachrichtenlänge prüfen
        if (nachrichtenTeile.length < 3) {
            ausgabe.println("REGISTER_FAIL|Invalides Format!");
            return;
        }

        String benutzername = nachrichtenTeile[1];
        String passwort = nachrichtenTeile[2];

        // schickt es zu UserDatabase.java weiter
        if (userDatenbank.registrieren(benutzername, passwort)) {
            ausgabe.println("REGISTER_OK");
        } else {
            ausgabe.println("REGISTER_FAIL|Username existiert bereits! Passwort eingeben!");
        }
    }

    // verarbeitet die Anmeldung des Clients [ChatClient.java]
    private void anmeldungBearbeiten(String[] p) {
        if (p.length < 3) {
            ausgabe.println("LOGIN_FAIL|Invalide Format!");
            return;
        }

        String benutzername = p[1];
        String passwort = p[2];

        // wenn anmelden falsch läuft = return + Fehler
        if (!userDatenbank.anmelden(benutzername, passwort)) {
            ausgabe.println("LOGIN_FAIL|Falsche Login-Daten");
            return;
        }

        username = benutzername;
        // speichert den User in der OnlineUser.java
        onlineUser.hinzufuegen(username, this);
        ausgabe.println("LOGIN_OK");
    }

    private void userlisteSenden() {
        String users = String.join(",", onlineUser.getUsernames());
        ausgabe.println("USERS|" + users);
    }


    // VERÄNDERUNG: Invite einseitig + RSA länge
    private void einladungBearbeiten(String[] p) {
        // Kontrolle für: CMD|User|Port|Key
        if (p.length < 4) {
            return;
        }

        String zielUser = p[1];
        int udpPort = Integer.parseInt(p[2]);
        String publicKey = p[3];

        ClientHandler zielHandler = onlineUser.getUserHandler(zielUser);
        if (zielHandler == null) {
            return;
        }
        
        String ipAdresse = socket.getInetAddress().getHostAddress();
        zielHandler.ausgabe.println("INVITE_FROM|" + username + "|" + ipAdresse + "|" + udpPort + "|" + publicKey);
    }

    // VERÄNDERUNG: neue Methode für einseitige optimierung + RSA
    private void inviteAcceptBearbeiten(String[] p) {
        // Kontrolle für: CMD|User|Port|Key
        if (p.length < 4) {
            return;
        }

        String einladenderUser = p[1];
        int udpPort = Integer.parseInt(p[2]);
        String publicKey = p[3];

        ClientHandler einladenderHandler = onlineUser.getUserHandler(einladenderUser);
        if (einladenderHandler != null) {
            String ipAdresse = socket.getInetAddress().getHostAddress();
            einladenderHandler.ausgabe.println("INVITE_ACCEPT_FROM|" + username + "|" + ipAdresse + "|" + udpPort + "|" + publicKey);
        }
    }
}