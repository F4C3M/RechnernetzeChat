package server;
import java.io.*;
import java.net.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private UserDatabase userDb = new UserDatabase();
    private OnlineUsers online = new OnlineUsers();

    public static void main(String[] args) {
        new ChatServer().start(5000);
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server läuft auf Port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new ClientHandler(client, userDb, online).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}