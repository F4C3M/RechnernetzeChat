package server;
import common.Encryption.*;

import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.SecretKey;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyString;
    private SecretKey clientAESKey;


    public ClientHandler(Socket socket, PrivateKey privateKey, PublicKey publicKey) {
        this.socket = socket;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());

    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            send("RSAKEY:" + publicKeyString);
            while (true) {

                while (clientAESKey == null){
                    String input = in.readLine();
                    if (input.startsWith("AESKEY:")) {
                        String encryptedBase64 = input.substring("AESKEY:".length());
                        byte[] encryptedKeyBytes = Base64.getDecoder().decode(encryptedBase64);
                        try {
                            byte[] decryptedKeyBytes = EncryptionHelper.decryptRSA(encryptedKeyBytes, privateKey);
                            clientAESKey = EncryptionHelper.decodeAESKey(decryptedKeyBytes);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                if (!handleInitialMenu()) {
                    
                }
                else{

                
                sendEncrypted("Successfully logged in as " + username);
                while (true) {
                    String encInput = in.readLine();
                    String input = EncryptionHelper.decryptAES(encInput, clientAESKey);
                    if (input == null || input.equalsIgnoreCase("quit")) {
                        System.out.println("Client durch 'quit' getrennt: " + username + " " + socket);
                        close();
                        return;
                    } else if (input.equalsIgnoreCase("list")) {
                        listActiveUsers();
                    } else if (input.startsWith("invite ")) {
                        String target = input.split(" ")[1];
                        ChatServer.setUdpPort(username, Integer.parseInt(input.split(" ")[2]) ); 
                        inviteUser(target);

                    } else if (input.contains("no") || input.contains("yes")){
                         handleInvitationResponse(input);
                    }else {

                    }
                }
                }
            }

        } catch (IOException e) {
            System.out.println("Client getrennt: " + username + " " + socket);
        } catch (Exception e) {
            // Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (username != null) {
                ChatServer.removeActiveUser(username);
            }
            close();
        }
    }

    private boolean handleInitialMenu() throws Exception {


        
        sendEncrypted("Willkommen. Bitte registrieren oder einloggen.");
        String input = in.readLine();
        String cmd = EncryptionHelper.decryptAES(input, clientAESKey);

        if (cmd == null) return false;

        if ("register".equalsIgnoreCase(cmd)) {
            
            
            return handleRegistration();
        } else if ("login".equalsIgnoreCase(cmd)) {
            return handleLogin(); 
        } 
        else {
            sendEncrypted("Ungültiger Befehl.");
            return false;
        }
    }


    private boolean handleRegistration() throws Exception {
        //send("Neuen Benutzernamen eingeben:");
        String encUser = in.readLine();
        String user = EncryptionHelper.decryptAES(encUser, clientAESKey);
        System.out.println(user);

        //send("Passwort eingeben:");
        String encPass = in.readLine();
        String pass = EncryptionHelper.decryptAES(encPass, clientAESKey);

        if (ChatServer.isRegistered(user)) {
            sendEncrypted("Benutzer existiert bereits!");
            return false;
        } else {
            ChatServer.registerUser(user, pass);
            this.username = user;
            ChatServer.addActiveUser(user, this);
            return true;
            
        }
        
    }


    private boolean handleLogin() throws Exception {
        //send("Neuen Benutzernamen eingeben:");
        String encUser = in.readLine();
        String user = EncryptionHelper.decryptAES(encUser, clientAESKey);

        //send("Passwort eingeben:");
        String encPass = in.readLine();
        String pass = EncryptionHelper.decryptAES(encPass, clientAESKey);

        if (!ChatServer.checkCredentials(user, pass)) {
            sendEncrypted("Login fehlgeschlagen.");
            return false;
        } else {
            this.username = user;
            ChatServer.addActiveUser(user, this);
            return true;
        }
    }


    private void listActiveUsers() throws IOException {
        var users = ChatServer.getActiveUsernames();
        System.out.println("Active Users auf Server: " + users);
        if (users.isEmpty()) {
            sendEncrypted("Keine aktiven Nutzer.");
        } else {
            sendEncrypted("Aktive Nutzer: " + users);
        }
    }

    private void inviteUser(String target) throws IOException {
        ClientHandler partner = ChatServer.getHandler(target);
        if (partner == null) {
            sendEncrypted("Benutzer nicht aktiv.");
            return;
        }
        ChatServer.pendingInvitations.put(target, username);
        partner.sendEncrypted(username + " invited you to chat.");
    }

    private void send(String message) throws IOException {
        out.write(message + "\n");
        out.flush();
    }

    private void sendEncrypted(String message) throws IOException {
    try {
        String encrypted = EncryptionHelper.encryptAES(message, clientAESKey);
        out.write(encrypted + "\n");
        out.flush();
    } catch (Exception ex) {
        System.out.println("Fehler beim AES-Senden an Client: " + ex.getMessage());
    }
}

    private void handleInvitationResponse(String response) throws IOException {
    String inviter = ChatServer.pendingInvitations.get(username); 
    if (inviter == null) {
        sendEncrypted("Keine ausstehende Einladung.");
        return;
    }

    ClientHandler inviterHandler = ChatServer.getHandler(inviter);
    if (inviterHandler == null) {
        sendEncrypted("Einladender Benutzer nicht verfügbar.");
        ChatServer.pendingInvitations.remove(username);
        return;
    }
    if (response.split(" ")[0].equals("yes")) {
        int port = Integer.parseInt(response.split(" ")[2]);
        ChatServer.setUdpPort(username, port); 
        sendEncrypted("Du bist jetzt mit " + inviter + " im Chat.");
        inviterHandler.sendEncrypted("Einladung akzeptiert. Du bist jetzt mit " + username + " im Chat.");

        String inviterIp = inviterHandler.getSocket().getInetAddress().getHostAddress();
        int inviterUdpPort = ChatServer.getUdpPort(inviter);

        String inviteeIp = this.socket.getInetAddress().getHostAddress();
        int inviteeUdpPort = ChatServer.getUdpPort(username);

        byte[] keyBytes = clientAESKey.getEncoded();
        String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);

        inviterHandler.sendEncrypted("peerinfo " + inviteeIp + " " + inviteeUdpPort + " " + keyBase64);
        this.sendEncrypted("peerinfo " + inviterIp + " " + inviterUdpPort + " " + keyBase64);

    } else {
        sendEncrypted("Einladung abgelehnt.");
        inviterHandler.sendEncrypted("Einladung von " + username + " abgelehnt.");
    }

    ChatServer.pendingInvitations.remove(username);
    }

    

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
