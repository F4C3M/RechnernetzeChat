package chat.server;
import java.io.*;
import java.net.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private UserDatabase userDatenbank = new UserDatabase();
    private OnlineUsers onlineUser = new OnlineUsers();

    public static void main(String[] args) {
        new ChatServer().start(5001);
    }

    public void start(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server läuft auf Port " + port);
            
            while (true) {
                Socket eingehenderClient = serverSocket.accept();
                new ClientHandler(eingehenderClient, userDatenbank, onlineUser).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}