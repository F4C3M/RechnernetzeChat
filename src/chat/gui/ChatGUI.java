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
    private JFrame loginFrame;
    private JFrame chatFrame;
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JTextArea chatArea = new JTextArea();
    private JTextField msgField = new JTextField();
    private UDPChat udpChat;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatGUI().showLogin());
    }

    public void showLogin() {
        loginFrame = new JFrame("Login / Register");
        loginFrame.setSize(350, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(4, 1));

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register");

        loginFrame.add(new JLabel("Username:"));
        loginFrame.add(userField);
        loginFrame.add(new JLabel("Passwort:"));
        loginFrame.add(passField);

        JPanel p = new JPanel();
        p.add(loginBtn);
        p.add(regBtn);
        loginFrame.add(p, BorderLayout.SOUTH);

        loginFrame.setVisible(true);

        // Connect to server
        client.connect("localhost", 5001);

        client.listenAsync(new ChatEvents() {
            @Override public void onUserList(List<String> users) {}
            @Override public void onInvite(String data) {}
            @Override public void onUdpMessage(String msg) {}
            @Override public void onDisconnect() {
                JOptionPane.showMessageDialog(
                    loginFrame,
                    "Server getrennt"
                );
            }
        });


        loginBtn.addActionListener(e -> {
            new Thread(() -> {
                try {
                    boolean ok = client.login(
                        userField.getText(),
                        new String(passField.getPassword())
                    );

                    SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            openChat();
                            loginFrame.dispose();
                        } else {
                            JOptionPane.showMessageDialog(
                                loginFrame,
                                "Falsche Login-Daten"
                            );
                        }
                    });

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        regBtn.addActionListener(e -> {
            new Thread(() -> {
                boolean ok = client.register(
                    userField.getText(),
                    new String(passField.getPassword())
                );

                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(
                            loginFrame,
                            "Registriert! Jetzt einloggen."
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                            loginFrame,
                            "Username existiert bereits."
                        );
                    }
                });
            }).start();
        });
    }

    public void openChat() {
        try {
            udpChat = new UDPChat(client.getUdpPort());
            udpChat.listen(new ChatEvents() {
                @Override public void onUdpMessage(String msg) {
                    SwingUtilities.invokeLater(() -> chatArea.append("\n" + msg));
                }
                @Override public void onUserList(List<String> u) {}
                @Override public void onInvite(String f) {}
                @Override public void onDisconnect() {}
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                chatFrame,
                "UDP-Start fehlgeschlagen: " + e.getMessage()
            );
            e.printStackTrace();
        }

        chatFrame = new JFrame("Chat");
        chatFrame.setSize(600, 400);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        userListModel.clear();
        JList<String> userList = new JList<>(userListModel);

        JButton refreshBtn = new JButton("Refresh Users");
        JButton inviteBtn = new JButton("Invite");

        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(msgField, BorderLayout.CENTER);
        JButton sendBtn = new JButton("Send (UDP)");
        bottom.add(sendBtn, BorderLayout.EAST);

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(userList), BorderLayout.CENTER);
        JPanel rightBtns = new JPanel();
        rightBtns.add(refreshBtn);
        rightBtns.add(inviteBtn);
        right.add(rightBtns, BorderLayout.SOUTH);

        chatFrame.setLayout(new BorderLayout());
        chatFrame.add(chatScroll, BorderLayout.CENTER);
        chatFrame.add(bottom, BorderLayout.SOUTH);
        chatFrame.add(right, BorderLayout.EAST);

        chatFrame.setVisible(true);

        client.listenAsync(new ChatEvents() {
            @Override
            public void onUserList(List<String> users) {
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    for (String u : users) userListModel.addElement(u);
                });
            }

            @Override
            public void onInvite(String data) {
                String[] p = data.split("\\|");
                String from = p[0];
                String ip = p[1];
                int port = Integer.parseInt(p[2]);

                int res = JOptionPane.showConfirmDialog(
                    chatFrame,
                    "Chat-Anfrage von " + from + " annehmen?",
                    "Invite",
                    JOptionPane.YES_NO_OPTION
                );

                if (res == JOptionPane.YES_OPTION) {
                    try {
                        udpChat.setTarget(InetAddress.getByName(ip), port);
                        chatArea.append("\nUDP verbunden mit " + from);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onUdpMessage(String msg) {
                SwingUtilities.invokeLater(() -> chatArea.append("\nUDP: " + msg));
            }

            @Override
            public void onDisconnect() { }
        });

        // buttons
        refreshBtn.addActionListener(e -> client.requestUserList());

        inviteBtn.addActionListener(e -> {
            String target = userList.getSelectedValue();
            if (target != null) client.sendInvite(target);
        });

        // UDP messages senden
        sendBtn.addActionListener(e -> sendMsg());
        msgField.addActionListener(e -> sendMsg());
    }

    private void sendMsg() {
        String msg = msgField.getText();
        msgField.setText("");

        chatArea.append("\n(" + client.getUsername() + "): " + msg);

        try {
            udpChat.sendMessage(client.getUsername() + ": " + msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}