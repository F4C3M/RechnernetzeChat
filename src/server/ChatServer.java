package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    static final int PORT = 6789;
    static Map<String, String> registeredUsers = new HashMap<>();
    static Map<String, ClientHandler> activeUsers = new HashMap<>();
    private static Map<String, Integer> udpPorts = new HashMap<>();

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);  
        KeyPair pair = gen.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server gestartet auf Port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Neuer Client verbunden: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, privateKey, publicKey)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static synchronized void registerUser(String username, String password) {
        registeredUsers.put(username, password);
    }

    static Map<String, String> pendingInvitations = new ConcurrentHashMap<>();

    static synchronized boolean isRegistered(String username) {
        return registeredUsers.containsKey(username);
    }

    static synchronized boolean checkCredentials(String username, String password) {
        return password.equals(registeredUsers.get(username));
    }

    static synchronized void addActiveUser(String username, ClientHandler handler) {
        activeUsers.put(username, handler);
    }

    static synchronized void removeActiveUser(String username) {
        activeUsers.remove(username);
    }
    
    public static void setUdpPort(String username, int port) {
        udpPorts.put(username, port);
    }

    public static int getUdpPort(String username) {
        return udpPorts.getOrDefault(username, -1);
    }

    static synchronized List<String> getActiveUsernames() {
        return new ArrayList<>(activeUsers.keySet());
    }

    static synchronized ClientHandler getHandler(String username) {
        return activeUsers.get(username);
    }
}

