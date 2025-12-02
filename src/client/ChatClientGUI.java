package client;
import common.Encryption.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyStore.SecretKeyEntry;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClientGUI {

    private static InetAddress peerAddress = null;
    private static int peerPort = -1;
    private static String peerUsername = "";
    private static PublicKey recipientPublicKey;
    private static SecretKey serverAES;
    private static SecretKey peerAES;
    private static ConcurrentHashMap<String, String> pendingMessages = new ConcurrentHashMap<>();


    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Chat Client");
        JTextArea chatArea = new JTextArea();
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Senden");
        JLabel activeUser = new JLabel("Not Logged in");
        JLabel loggedInAs = new JLabel("Logged in as: ");
        BlockingQueue<String> invitationQueue = new LinkedBlockingQueue<>();
        inputField.setEnabled(false);
        sendButton.setEnabled(false);   
        
        chatArea.setEditable(false);
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        frame.add(panel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Aktionen"));

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");
        JButton refreshButton = new JButton("Refresh User List");
        refreshButton.setEnabled(false);

        //DefaultListModel<String> userListModel = new DefaultListModel<>();
        //JList<String> userList = new JList<>(userListModel);
        rightPanel.add(loggedInAs);
        rightPanel.add(activeUser);
        rightPanel.add(Box.createRigidArea(new Dimension(0,15)));
        rightPanel.add(registerButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0,3)));
        rightPanel.add(loginButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0,20)));
        rightPanel.add(refreshButton, BorderLayout.EAST);
        rightPanel.add(Box.createRigidArea(new Dimension(0,3)));
        rightPanel.add(new JLabel("Invite Active Users:"), BorderLayout.NORTH);
        //rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        JScrollPane userScrollPane = new JScrollPane(userListPanel);
        userScrollPane.setPreferredSize(new Dimension(100, 150));
        rightPanel.add(userScrollPane);

        frame.add(rightPanel, BorderLayout.EAST);

        frame.setSize(700, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Socket socket = new Socket("localhost", 6789);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        DatagramSocket udpSocket = new DatagramSocket();
        int udpPort = udpSocket.getLocalPort();
        new Thread(() -> {
            try {
                while (true) {
                    String invitation = invitationQueue.take();
                    peerUsername = invitation.split(" ")[0];
                    SwingUtilities.invokeAndWait(() -> {
                        int choice = JOptionPane.showConfirmDialog(
                            frame,
                            invitation + "\nMöchtest du annehmen?",
                            "Einladung",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        );

                        try {
                            if (choice == JOptionPane.YES_OPTION) {
                                out.write(EncryptionHelper.encryptAES("yes " + "udpport " + udpPort, serverAES)+ "\n");
                                out.flush();
                            } else {
                                out.write(EncryptionHelper.encryptAES("no", serverAES)+ "\n");
                                out.flush();
                            }
                        } catch (Exception ex) {
                            chatArea.append("Fehler beim Antworten auf Einladung.\n");
                        }
                    });
                }
            } catch (InterruptedException | InvocationTargetException e) {

            }
        }).start();

        new Thread(() -> {
            String msg;
            
            try {
                while ((msg = in.readLine()) != null) {
                    if(serverAES == null){
                        String receivedKeyString = msg.substring("RSAKEY:".length());
                        recipientPublicKey = EncryptionHelper.RSAStringToPublicKey(receivedKeyString);
                        serverAES = EncryptionHelper.generateAESKey();
                        byte[] aesKeyBytes = serverAES.getEncoded();
                        byte[] encryptedAESKey = EncryptionHelper.encryptRSA(aesKeyBytes, recipientPublicKey);
                        String encryptedAESKeyBase64 = Base64.getEncoder().encodeToString(encryptedAESKey);
                        out.write("AESKEY:"+encryptedAESKeyBase64+"\n");
                        out.flush();
                    }else{
                        msg = EncryptionHelper.decryptAES(msg, serverAES);

                        if(peerAddress != null)
                            break;
                        if (msg.startsWith("Aktive Nutzer:")) {
                            chatArea.append("Successfully Refreshed User List.\n");
                            final String userMsg = msg; 
                            SwingUtilities.invokeLater(() -> {
                                userListPanel.removeAll();
                                int start = userMsg.indexOf("[");
                                int end = userMsg.indexOf("]");
                                if (start != -1 && end != -1 && end > start) {
                                    String listPart = userMsg.substring(start + 1, end);
                                    String[] users = listPart.split(",");
                                    for (String user : users) {
                                        final String trimmedUser = user.trim(); 
                                        JButton inviteButton = new JButton(trimmedUser);
                                        if(trimmedUser.equals(activeUser.getText()))
                                            inviteButton.setEnabled(false);
                                        inviteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, inviteButton.getPreferredSize().height));
                                        inviteButton.addActionListener(ev -> {
                                            try {
                                                peerUsername = trimmedUser;
                                                out.write(EncryptionHelper.encryptAES("invite " + trimmedUser + " " + udpPort, serverAES)+ "\n");
                                                out.flush();
                                                chatArea.append("Invite an " + trimmedUser + " gesendet.\n");
                                            } catch (Exception ex) {
                                                chatArea.append("Fehler beim Invite.\n");
                                            }
                                        });
                                        userListPanel.add(inviteButton);
                                    }
                                }
                                userListPanel.revalidate();
                                userListPanel.repaint();
                            });
                        }else if(msg.contains("peerinfo ")){
                                String[] parts = msg.split(" ");
                                String peerIp = parts[1];
                                peerPort = Integer.parseInt(parts[2]);
                                String peerAESBase64 = parts[3];
                                byte[] decodedKey = Base64.getDecoder().decode(peerAESBase64);
                                peerAES = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

                                peerAddress = InetAddress.getByName(peerIp);
                                peerPort = Integer.parseInt(parts[2]);
                                userListPanel.removeAll();
                                userListPanel.setEnabled(false);
                                userScrollPane.setEnabled(false);
                                refreshButton.setEnabled(false);

                                new Thread(() -> {
                                    byte[] buffer = new byte[1024];
                                    while (true) {
                                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                                        try {
                                            udpSocket.receive(packet);
                                            String received = new String(packet.getData(), 0, packet.getLength());


                                            if (received.startsWith("ACK:")) {
                                                String ackId = received.substring(4);
                                                pendingMessages.remove(ackId);  // ACK erhalten
                                                continue;
                                            }


                                            final String decryptedReceived = EncryptionHelper.decryptAES(received.substring(received.indexOf(":")+1), peerAES);
                                            String messageId = received.split(":")[0];


                                            String ack = "ACK:" + messageId;
                                            byte[] ackData = ack.getBytes();
                                            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, peerAddress, peerPort);
                                            udpSocket.send(ackPacket);


                                            SwingUtilities.invokeLater(() -> {
                                                chatArea.append("[" + peerUsername + "] " + decryptedReceived + "\n");
                                            });

                                        } catch (Exception e) {
                                            break;
                                        }
                                    }
                                }).start();
                        }
                        
                        else if (msg.contains("invited you")) {
                            invitationQueue.offer(msg);
                        }
                        
                        else{
                                chatArea.append("SERVER: " + msg + "\n");
                        }

                        if (msg.toLowerCase().contains("successfully logged in")) {
                            
                            activeUser.setText(msg.split(" ")[4]);
                            registerButton.setEnabled(false);
                            loginButton.setEnabled(false);
                            inputField.setEnabled(true);
                            sendButton.setEnabled(true);
                            refreshButton.setEnabled(true);
                        }
                    }

                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Verbindung verloren.\n");
                    frame.dispose();
                    System.exit(0);
                });
            } catch (Exception e) {

                e.printStackTrace();
            }
        }).start();

        Runnable sendMessage = () -> {
            String text = inputField.getText();
            String plainText = text;

            try {
                    if (peerAddress != null) {
                        String id = UUID.randomUUID().toString();
                        String encrypted = EncryptionHelper.encryptAES(plainText, peerAES);
                        String message = id + ":" + encrypted;

                        byte[] data = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
                        pendingMessages.put(id, message);


                        new Thread(() -> {
                            try {
                                for (int i = 0; i < 5; i++) {  
                                    if (!pendingMessages.containsKey(id)) break;  
                                    udpSocket.send(packet);
                                    Thread.sleep(1000);  
                                }
                                if (pendingMessages.containsKey(id)) {
                                    SwingUtilities.invokeLater(() -> {
                                        chatArea.append("[SYSTEM] Nachricht wurde nicht bestätigt.\n");
                                    });
                                    pendingMessages.remove(id);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

            chatArea.append("[" + activeUser.getText() + "] " + plainText + "\n");

                    registerButton.setEnabled(false);
                    loginButton.setEnabled(false);
                    refreshButton.setEnabled(false);
                } else {
                    out.write(EncryptionHelper.encryptAES(text, serverAES)+ "\n" );
                    out.flush();
                }

                inputField.setText("");

                if (text.equalsIgnoreCase("quit")) {
                    socket.close();
                    udpSocket.close();
                    frame.dispose();
                    System.exit(0);
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Fehler beim Senden.\n");
                    frame.dispose();
                    System.exit(0);
                });
            }
        };

        inputField.addActionListener(e -> sendMessage.run());
        sendButton.addActionListener(e -> sendMessage.run());

        registerButton.addActionListener(e -> {

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame, "Register", true);
            dialog.setLayout(new GridLayout(3, 2, 5, 5));

            JLabel userLabel = new JLabel("Username:");
            JTextField userField = new JTextField();
            JLabel passLabel = new JLabel("Password:");
            JPasswordField passField = new JPasswordField();

            JButton submitButton = new JButton("Senden");

            dialog.add(userLabel);
            dialog.add(userField);
            dialog.add(passLabel);
            dialog.add(passField);
            dialog.add(new JLabel());
            dialog.add(submitButton);


             Runnable submitAction = () -> {
                {
                String username = userField.getText();
                String password = new String(passField.getPassword());
                try {
                    out.write(EncryptionHelper.encryptAES("register",serverAES)+ "\n");
                    System.out.println(EncryptionHelper.encryptAES("register\n",serverAES));
                    System.out.println(EncryptionHelper.decryptAES(EncryptionHelper.encryptAES("register\n",serverAES), serverAES));
                    out.flush();
                    out.write(EncryptionHelper.encryptAES(username,serverAES)+ "\n");
                    out.flush();
                    out.write(EncryptionHelper.encryptAES(password,serverAES)+ "\n");
                    out.flush();
                    dialog.dispose();
                } catch (Exception ex) {
                    chatArea.append("Fehler beim Registrieren.\n");
                }
            }
            };

            
            passField.addActionListener(ev -> submitAction.run()); 
            userField.addActionListener(ev -> submitAction.run());

            submitButton.addActionListener(ev -> submitAction.run());

            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });

        });

        loginButton.addActionListener(e -> {

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame, "Login", true);
            dialog.setLayout(new GridLayout(3, 2, 5, 5));

            JLabel userLabel = new JLabel("Username:");
            JTextField userField = new JTextField();
            JLabel passLabel = new JLabel("Password:");
            JPasswordField passField = new JPasswordField();

            JButton submitButton = new JButton("Senden");

            dialog.add(userLabel);
            dialog.add(userField);
            dialog.add(passLabel);
            dialog.add(passField);
            dialog.add(new JLabel());
            dialog.add(submitButton);


             Runnable submitAction = () -> {
                {
                String username = userField.getText();
                String password = new String(passField.getPassword());
                try {
                    out.write(EncryptionHelper.encryptAES("login", serverAES)+ "\n");
                    out.flush();
                    out.write(EncryptionHelper.encryptAES(username, serverAES)+ "\n");
                    out.flush();
                    out.write(EncryptionHelper.encryptAES(password, serverAES)+ "\n");
                    out.flush();
                    dialog.dispose();
                } catch (Exception ex) {
                    chatArea.append("Fehler beim Login.\n");
                }
            }
            };

            
            passField.addActionListener(ev -> submitAction.run()); 
            userField.addActionListener(ev -> submitAction.run());

            submitButton.addActionListener(ev -> submitAction.run());

            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);

        });

        });

        refreshButton.addActionListener(e -> {
            try {
                out.write(EncryptionHelper.encryptAES("list", serverAES)+ "\n");
                out.flush();
            } catch (Exception ex) {
                chatArea.append("Fehler beim Refresh.\n");
            }
        });
    }
}
