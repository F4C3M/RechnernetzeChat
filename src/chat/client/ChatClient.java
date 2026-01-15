package chat.client;
import java.io.*;
import java.net.*;
import java.util.*;

// IST BRÜCK ZW. GUI und SERVER [TCP]
// stellt Verbindung zum Server her
// sendet die Nachrichten
// verabeitung der Servernachrichten
public class ChatClient {
    private Socket socket; // "Steckdose" | 2 ComProg Daten aus.
    private BufferedReader eingabe;
    private PrintWriter ausgabe;
    
    private String username;
    private int udpPort;

    private final Object sperreAntwort = new Object();
    private String letzteAntwort;

    public boolean verbinden(String host, int port) {
        try {
            socket = new Socket(host, port);
            eingabe = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ausgabe = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // [GUI aus], sendet Nachricht an Server [TCP]
    public boolean registrieren(String username, String passwort) {
        synchronized (sperreAntwort) {
            ausgabe.println("REGISTER|" + username + "|" + passwort);
            
            // damit keine alte Antwort den Client verwirrt
            letzteAntwort = null;
            // wartet auf Antwort aus serverMessageVerarbeiten() [if]
            while (letzteAntwort == null) {
                try {
                    sperreAntwort.wait();
                } catch (InterruptedException ignored) {}
            }
            // vergleicht mit der "letztenAntwort" vom Server [ClientHandler.java/registrierungBearbeiten]
            return letzteAntwort.equals("REGISTER_OK");
        }
    }

    // [GUI aus], sendet Nachricht an Server [TCP]
    public boolean anmelden(String username, String passwort) throws IOException {
        synchronized (sperreAntwort) {
            ausgabe.println("LOGIN|" + username + "|" + passwort);
            letzteAntwort = null;

            // wartet, bis ClientHandler.java, UserDatabase.java, OnlineUser.java durch sind
            while (letzteAntwort == null) {
                try {
                    sperreAntwort.wait();
                } catch (InterruptedException ignored) {}
            }

            if (letzteAntwort.equals("LOGIN_OK")) {
                this.username = username;
                this.udpPort = 6000 + new Random().nextInt(1000);
                return true;
            }
            return false;
        }
    }

    // gibt die Userliste aus für refreshButton [TCP]
    public void userlisteAnfordern() {
        ausgabe.println("GET_USERS");
    }

    // ist die Methode für den Invite Button [TCP]
    public void einladungSenden(String user, String meinPublicKey) {
        ausgabe.println("INVITE|" + user + "|" + udpPort + "|" + meinPublicKey);
    }

    public void serverListenerStarten(ChatEvents events) {
        new Thread(() -> {
            try {
                String serverNachricht;

                while ((serverNachricht = eingabe.readLine()) != null) {
                    serverMessageVerarbeiten(serverNachricht, events);
                }
            } catch (IOException e) {
                events.beiTrennung();
            }
        }).start();
    }

    // verarbeitet die Message vom Server [ClientHandler.java] (INVITE_FROM)
    private void serverMessageVerarbeiten(String nachricht, ChatEvents events) {
        System.out.println("RECV: " + nachricht);

        if (nachricht.startsWith("LOGIN_") || nachricht.startsWith("REGISTER_")) {
            synchronized (sperreAntwort) {
                letzteAntwort = nachricht;
                sperreAntwort.notifyAll();
            }
            return;
        }

        String[] nachrichtenTeile = nachricht.split("\\|");
        switch (nachrichtenTeile[0]) {
            case "USERS":
                events.beiUserliste(Arrays.asList(nachrichtenTeile[1].split(",")));
                break;
            case "INVITE_FROM":
            case "INVITE_ACCEPT_FROM":
                // komplette Nachricht weitergeben
                events.beiEinladung(nachricht);
                break;
        }
    }

    // VERÄNDERUNG: neue Methode
    public void sendeAusgabeAnServer(String nachricht) {
        ausgabe.println(nachricht);
    }

    // gibt Username zurück
    public String getUsername() {
        return username;
    }

    // gibt Port zurück
    public int getUdpPort() {
        return udpPort;
    }
}