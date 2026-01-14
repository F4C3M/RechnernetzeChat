package chat.client;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private Socket socket;
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

    public boolean registrieren(String username, String passwort) {
        synchronized (sperreAntwort) {
            ausgabe.println("REGISTER|" + username + "|" + passwort);
            letzteAntwort = null;

            while (letzteAntwort == null) {
                try {
                    sperreAntwort.wait();
                } catch (InterruptedException ignored) {}
            }
            return letzteAntwort.equals("REGISTER_OK");
        }
    }

    public boolean anmelden(String username, String passwort) throws IOException {
        synchronized (sperreAntwort) {
            ausgabe.println("LOGIN|" + username + "|" + passwort);
            letzteAntwort = null;

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

    public void userlisteAnfordern() {
        ausgabe.println("GET_USERS");
    }

    // VERÄNDERUNG: RSA angepasst durch "meinPublicKey"
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

    // VERÄNDERUNG: case INVITE_FROM | case INVITE_ACCEPT_FROM
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

    public String getUsername() {
        return username;
    }

    public int getUdpPort() {
        return udpPort;
    }
}