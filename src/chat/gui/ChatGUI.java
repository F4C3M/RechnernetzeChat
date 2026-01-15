package chat.gui;
import javax.swing.*;
import chat.client.ChatClient;
import chat.client.ChatEvents;
import chat.client.RSAManager;
import chat.client.UDPChat;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.List;

public class ChatGUI {
    private ChatClient client = new ChatClient();
    private JFrame loginFenster;
    private JFrame chatFenster;
    private DefaultListModel<String> userlistenModell = new DefaultListModel<>();
    private JTextArea chatTextbereich = new JTextArea();
    private JTextField nachrichtenFeld = new JTextField();
    private UDPChat udpChat;

    // neue Variablen für das RSA
    private RSAManager rsa = new RSAManager();
    private PublicKey partnerPublicKey;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatGUI().loginFensterStarten());
    }

    // erstellen des Loginfensters + starten der Listener (Server / Button)
    public void loginFensterStarten() {
        // GUI Elemente
        loginFenster = new JFrame("Login / Register");
        loginFenster.setSize(350, 200);
        loginFenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFenster.setLayout(new GridLayout(4, 1));

        // Felder / Buttons erstellen / initialiseieren
        JTextField benutzerFeld = new JTextField();
        JPasswordField passwortFeld = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        // Felder / Buttons in das "loginFenster" einfügen
        loginFenster.add(new JLabel("Username:"));
        loginFenster.add(benutzerFeld);
        loginFenster.add(new JLabel("Passwort:"));
        loginFenster.add(passwortFeld);

        JPanel panel = new JPanel();
        panel.add(loginButton);
        panel.add(registerButton);
        loginFenster.add(panel, BorderLayout.SOUTH);

        loginFenster.setVisible(true);
        client.verbinden("localhost", 5001);


        // Listener für den Server starten (verarbeitet die ChatEvents)
        client.serverListenerStarten(new ChatEvents() {
            @Override public void beiUserliste(List<String> users) {
                if (userlistenModell != null) {
                    SwingUtilities.invokeLater(() -> {
                        userlistenModell.clear();
                        for (String u : users) userlistenModell.addElement(u);
                    });
                }
            }
            @Override public void beiEinladung(String daten) {
                handleEinladungProtokoll(daten);
            }
            @Override public void beiUdpNachricht(String nachrichten) {
                handleUdpProtokoll(nachrichten);
            }
            @Override public void beiTrennung() {
                JOptionPane.showMessageDialog(loginFenster, "Server wurde getrennt");
            }
        });

        // Listener für den "loginButton" (startet die anmelde Welle)
        loginButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    boolean erfolgreich = client.anmelden(
                        benutzerFeld.getText(),
                        new String(passwortFeld.getPassword())
                    );

                    SwingUtilities.invokeLater(() -> {
                        if (erfolgreich) {
                            chatFensterStarten();
                            loginFenster.dispose();
                        } else {
                            JOptionPane.showMessageDialog(loginFenster, "Logindaten sind falsch!");
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        // Listener für den "registerButton" (startet die registrierungs Welle)
        registerButton.addActionListener(e -> {
            new Thread(() -> {
                boolean erfolgreich = client.registrieren(
                    benutzerFeld.getText(),
                    new String(passwortFeld.getPassword())
                );

                SwingUtilities.invokeLater(() -> {
                    if (erfolgreich) {
                        JOptionPane.showMessageDialog(loginFenster, "Registrierung erfolgt! Loggen Sie sich ein.");
                    } else {
                        JOptionPane.showMessageDialog(loginFenster, "Username vergeben! Passwort eingeben!");
                    }
                });
            }).start();
        });
    }

    // erstellen des Chatfensters
    public void chatFensterStarten() {
        try {
            udpChat = new UDPChat(client.getUdpPort());
            // UDP-Thread Listener wird gestartet (Methode in UDPChat)
            udpChat.listen(new ChatEvents() {
                @Override public void beiUdpNachricht(String nachricht) {
                    handleUdpProtokoll(nachricht);
                }
                @Override public void beiUserliste(List<String> userliste) {}
                @Override public void beiEinladung(String einladungsDaten) {}
                @Override public void beiTrennung() {}
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(chatFenster, "UDP-Start fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }

        // GUI Elemente
        chatFenster = new JFrame("Chat von: " + client.getUsername()) ;
        chatFenster.setSize(600, 400);
        chatFenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        userlistenModell.clear();
        JList<String> userliste = new JList<>(userlistenModell);
        JButton refreshButton = new JButton("User aktualisieren");
        JButton inviteButton = new JButton("Einladen");

        chatTextbereich.setEditable(false);
        JScrollPane chatScrollen = new JScrollPane(chatTextbereich);

        JPanel untenPanel = new JPanel(new BorderLayout());
        untenPanel.add(nachrichtenFeld, BorderLayout.CENTER);
        
        JButton sendenButton = new JButton("Send");
        untenPanel.add(sendenButton, BorderLayout.EAST);

        JPanel rechtsPanel = new JPanel(new BorderLayout());
        rechtsPanel.add(new JScrollPane(userliste), BorderLayout.CENTER);
        
        JPanel rechteButtons = new JPanel();
        rechteButtons.add(refreshButton);
        rechteButtons.add(inviteButton);
        rechtsPanel.add(rechteButtons, BorderLayout.SOUTH);

        chatFenster.setLayout(new BorderLayout());
        chatFenster.add(chatScrollen, BorderLayout.CENTER);
        chatFenster.add(untenPanel, BorderLayout.SOUTH);
        chatFenster.add(rechtsPanel, BorderLayout.EAST);
        chatFenster.setVisible(true);

        // Button Listener [RSA UPDATE]
        refreshButton.addActionListener(e -> client.userlisteAnfordern());
        inviteButton.addActionListener(e -> {
            String zielUser = userliste.getSelectedValue();
            if (zielUser != null) {
                client.einladungSenden(zielUser, rsa.publicKeyEncoder());
            }
        });

        // UDP messages senden
        sendenButton.addActionListener(e -> nachrichtenSenden());
        nachrichtenFeld.addActionListener(e -> nachrichtenSenden());
    }

    // NOCH KOMMENTIEREN!!! [RSA UPDATE]
    private void nachrichtenSenden() {
        String nachricht = nachrichtenFeld.getText().trim();
        
        if (nachricht.isEmpty()) {
            return;
        }

        if (partnerPublicKey == null) {
            chatTextbereich.append("\nWarten auf Verschlüsselungs-Key vom Partner...");
            return;
        }
        
        try {
            // Nachricht mit dem publicKeyVomPartner verschlüsseln
            String geheim = rsa.encrypt(nachricht, partnerPublicKey);

            // über UDP an den partner User verschicken
            udpChat.nachrichtSenden("MSG|" + client.getUsername() + ": " + geheim);
            
            // im eingenen Fenster eigene Nachricht sehen
            chatTextbereich.append("\n" + client.getUsername() + ": " + nachricht);
            
            // zum leeren des Felds
            nachrichtenFeld.setText("");
        } catch (Exception e) {
            chatTextbereich.append("\nFehler beim Verschlüsseln!");
            e.printStackTrace();
        }
    }

    // VERÄNDERUNG: neue / andere / ausgelagerte Methoden
    private void handleEinladungProtokoll(String daten) {
        String[] paket = daten.split("\\|");
        System.out.println("DEBUG-TCP: " + daten);

        if (daten.startsWith("INVITE_FROM|")) {
            // User2 (Verbindng annehmen)
            handleInviteFrom(paket);
        } 
        else if (daten.startsWith("INVITE_ACCEPT_FROM|")) {
            // User1 (Verbindung herstellen)
            handleInviteAccept(paket);
        }
    }

    // VERÄNDERUNG: RSA
    private void handleInviteFrom(String[] paket) {
        String vonUser = paket[1];
        String ip = paket[2];
        int port = Integer.parseInt(paket[3]);
        String publicKeyVomPartner = paket[4];

        int antwort = JOptionPane.showConfirmDialog(chatFenster, 
                "Einladung von " + vonUser + " annehmen?", "Invite", JOptionPane.YES_NO_OPTION);

        if (antwort == JOptionPane.YES_OPTION) {
            try {
                // User1 Key speichern
                partnerPublicKey = RSAManager.publicKeyDecoder(publicKeyVomPartner);
                System.out.println("DEBUG: Public Key von " + vonUser + " erhalten und gespeichert.");

                // User2 setzt User1 als Ziel fest
                udpChat.zielFestlegen(InetAddress.getByName(ip), port);
                udpChat.nachrichtSenden("HELLO");

                // zurück zum Server schicken
                client.sendeAusgabeAnServer("INVITE_ACCEPT|" + vonUser + "|" + client.getUdpPort() + "|" + rsa.publicKeyEncoder());                
                
                SwingUtilities.invokeLater(() -> chatTextbereich.append("\nSie sind mit '" + vonUser + "' verbunden."));
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(chatFenster, "Kryptographie-Fehler beim Key-Austausch!");
            }
        }
    }

    // VERÄNDERUNG: RSA
    private void handleInviteAccept(String[] paket) {
        try {
            String vonUser = paket[1];
            String ip = paket[2];
            int port = Integer.parseInt(paket[3]);
            String publicKeyVomPartner = paket[4];

            System.out.println("DEBUG: User1 setzt jetzt Ziel auf " + ip + ":" + port);

            // sicherstellen, dass udpChat bereit ist
            if (udpChat != null) {
                // User2 Key speichern
                partnerPublicKey = RSAManager.publicKeyDecoder(publicKeyVomPartner);
                System.out.println("DEBUG: Public Key von " + vonUser + " erhalten.");

                udpChat.zielFestlegen(InetAddress.getByName(ip), port);
                udpChat.nachrichtSenden("HELLO");

                SwingUtilities.invokeLater(() -> chatTextbereich.append("\nSie sind mit '" + vonUser + "' verbunden."));
            } else {
                System.out.println("DEBUG-FAIL: udpChat war noch null!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // VERÄNDERUNG: RSA
    private void handleUdpProtokoll(String nachricht) {
        // 1. Handshake Filter
        if (nachricht.equals("HELLO") || nachricht.equals("HELLO_ACK")) {
            if (nachricht.equals("HELLO")) {
                try {
                    udpChat.nachrichtSenden("HELLO_ACK");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        // 2. Verarbeitung verschlüsselter Nachrichten
        SwingUtilities.invokeLater(() -> {
            try {
                String inhalt = nachricht;
                if (nachricht.startsWith("MSG|")) {
                    inhalt = nachricht.substring(4);
                }

                // trennen von Name & verschlü. Text
                // Voraussetzung "Name: Geheimnis"
                String[] teile = inhalt.split(": ", 2);
                if (teile.length < 2) {
                    chatTextbereich.append("\n" + inhalt);
                    return;
                }

                String absender = teile[0];
                String verschluesselt = teile[1];

                String klartext = rsa.decrypt(verschluesselt);
                chatTextbereich.append("\n" + absender + ": " + klartext);
                
            } catch (Exception e) {
                chatTextbereich.append("\nNachricht empfangen, aber Entschlüsselung fehlgeschlagen!");
                System.err.println("RSA-Fehler: " + e.getMessage());
            }
        });
    }


    // Over-all-VERÄNDERUNGEN:
    // nur noch ein Listener (in loginFensterStarten())
    // in loginFenserStarten() client.serverListenerStarten umgeschrieben (viel) 
    // in loginFensterStarten()/@Override beiUserlist (wird nur geupdatet, wenn das Modell schon existiert)
    // in chatFensterStarten() initialisieren wir NUR NOCH die UDP-Komponente und das Fenster
    // Methoden aus chatFensterStarten() rausgenommen (in die allgemeine Klasse)
    // insgesamte Logik nach unten ausgelagert
}