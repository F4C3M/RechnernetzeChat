package chat.server;
import java.util.*;
import java.util.concurrent.*;

// speichert den User & Hash in einer Liste oder Datei (online)
class OnlineUsers {
    private final Map<String, ClientHandler> handlers = new ConcurrentHashMap<>();

    // kann zur OnlineUser Liste hinzufügen [ClientHandler.java/anmeldungBearbeiten()]
    public void hinzufuegen(String username, ClientHandler userHandler) {
        handlers.put(username, userHandler);
    }

    // kann von OnlineUser Liste entfernen [ClientHandler.java/run()]
    public void entfernen(String username) {
        handlers.remove(username);
    }

    
    public Set<String> getUsernames() {
        return handlers.keySet();
    }

    public ClientHandler getUserHandler(String username) {
        return handlers.get(username);
    }
}