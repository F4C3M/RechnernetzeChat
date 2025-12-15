package server;
import java.util.*;
import java.util.concurrent.*;

class OnlineUsers {
    private final Map<String, ClientHandler> handlers = new ConcurrentHashMap<>();

    public void add(String username, ClientHandler handler) {
        handlers.put(username, handler);
    }

    public void remove(String username) {
        handlers.remove(username);
    }

    public Set<String> getUsernames() {
        return handlers.keySet();
    }

    public ClientHandler getHandler(String username) {
        return handlers.get(username);
    }
}