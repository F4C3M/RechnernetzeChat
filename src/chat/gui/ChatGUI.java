package chat.gui;
import javax.swing.*;
import chat.client.ChatClient;
import chat.client.ChatEvents;
import chat.client.UDPChat;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class ChatGUI {
    private ChatClient client = new ChatClient();
    private JFrame loginFenster;
    private JFrame chatFenster;
    private DefaultListModel<String> userlistenModell = new DefaultListModel<>();
    private JTextArea chatTextbereich = new JTextArea();
    private JTextField nachrichtenFeld = new JTextField();
    private UDPChat udpChat;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatGUI().zeigLoginFenster());
    }

    public void zeigLoginFenster() {
        loginFenster = new JFrame("Login / Register");
        loginFenster.setSize(350, 200);
        loginFenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFenster.setLayout(new GridLayout(4, 1));

        JTextField benutzerFeld = new JTextField();
        JPasswordField passwortFeld = new JPasswordField();

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

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

        client.serverListenerStarten(new ChatEvents() {
            @Override public void beiUserliste(List<String> users) {}
            @Override public void beiEinladung(String daten) {}
            @Override public void beiUdpNachricht(String nachrichten) {}
            @Override public void beiTrennung() {
                JOptionPane.showMessageDialog(
                    loginFenster,
                    "Server wurde getrennt"
                );
            }
        });

        loginButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    boolean erfolg = client.anmelden(
                        benutzerFeld.getText(),
                        new String(passwortFeld.getPassword())
                    );

                    SwingUtilities.invokeLater(() -> {
                        if (erfolg) {
                            chatOeffnen();
                            loginFenster.dispose();
                        } else {
                            JOptionPane.showMessageDialog(
                                loginFenster,
                                "Falsche Logindaten!"
                            );
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        registerButton.addActionListener(e -> {
            new Thread(() -> {
                boolean erfolg = client.registrieren(
                    benutzerFeld.getText(),
                    new String(passwortFeld.getPassword())
                );

                SwingUtilities.invokeLater(() -> {
                    if (erfolg) {
                        JOptionPane.showMessageDialog(
                            loginFenster,
                            "Sie sind jetzt registriert! Jetzt loggen Sie sich ein."
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                            loginFenster,
                            "Der Username existiert bereits."
                        );
                    }
                });
            }).start();
        });
    }

    public void chatOeffnen() {
        try {
            udpChat = new UDPChat(client.getUdpPort());
            udpChat.lauschen(new ChatEvents() {
                @Override public void beiUdpNachricht(String nachricht) {
                    SwingUtilities.invokeLater(() -> chatTextbereich.append("\n" + nachricht));
                }
                @Override public void beiUserliste(List<String> userliste) {}
                @Override public void beiEinladung(String einladungsDaten) {}
                @Override public void beiTrennung() {}
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                chatFenster,
                "UDP-Start fehlgeschlagen: " + e.getMessage()
            );
            e.printStackTrace();
        }

        chatFenster = new JFrame("Chat");
        chatFenster.setSize(600, 400);
        chatFenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        userlistenModell.clear();
        JList<String> userliste = new JList<>(userlistenModell);
        JButton refreshButton = new JButton("Refresh Users");
        JButton inviteButton = new JButton("Invite");

        chatTextbereich.setEditable(false);
        JScrollPane chatScrollen = new JScrollPane(chatTextbereich);

        JPanel untenPanel = new JPanel(new BorderLayout());
        untenPanel.add(nachrichtenFeld, BorderLayout.CENTER);
        
        JButton sendenButton = new JButton("Send (UDP)");
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

        client.serverListenerStarten(new ChatEvents() {
            @Override
            public void beiUserliste(List<String> users) {
                SwingUtilities.invokeLater(() -> {
                    userlistenModell.clear();
                    for (String user : users) userlistenModell.addElement(user);
                });
            }

            @Override
            public void beiEinladung(String daten) {
                String[] paket = daten.split("\\|");
                String vonBenutzer = paket[0];
                String ipAdresse = paket[1];
                int port = Integer.parseInt(paket[2]);

                int antwortDialog = JOptionPane.showConfirmDialog(
                    chatFenster,
                    "Chat-Anfrage von " + vonBenutzer + " annehmen?",
                    "Invite",
                    JOptionPane.YES_NO_OPTION
                );

                if (antwortDialog == JOptionPane.YES_OPTION) {
                    try {
                        udpChat.zielFestlegen(InetAddress.getByName(ipAdresse), port);
                        chatTextbereich.append("\nUDP verbunden mit " + vonBenutzer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void beiUdpNachricht(String nachricht) {
                SwingUtilities.invokeLater(() -> chatTextbereich.append("\nUDP: " + nachricht));
            }

            @Override
            public void beiTrennung() { }
        });

        // buttons
        refreshButton.addActionListener(e -> client.userlisteAnfordern());
        inviteButton.addActionListener(e -> {
            String zielUser = userliste.getSelectedValue();
            if (zielUser != null) client.einladungSenden(zielUser);
        });

        // UDP messages senden
        sendenButton.addActionListener(e -> nachrichtenSenden());
        nachrichtenFeld.addActionListener(e -> nachrichtenSenden());
    }

    private void nachrichtenSenden() {
        String nachricht = nachrichtenFeld.getText();
        nachrichtenFeld.setText("");
        chatTextbereich.append("\n(" + client.getUsername() + "): " + nachricht);

        try {
            udpChat.nachrichtSenden(client.getUsername() + ": " + nachricht);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}