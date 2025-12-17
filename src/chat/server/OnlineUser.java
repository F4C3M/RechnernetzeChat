package chat.server;
import java.util.*;
import java.util.concurrent.*;

class OnlineUsers {
    private final Map<String, ClientHandler> handlers = new ConcurrentHashMap<>();

    public void hinzufuegen(String username, ClientHandler userHandler) {
        handlers.put(username, userHandler);
    }

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