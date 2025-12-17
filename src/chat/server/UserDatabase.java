package chat.server;
import java.util.*;
import java.util.concurrent.*;

class UserDatabase {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, password);
        return true;
    }

    public boolean login(String username, String password) {
        return password.equals(users.get(username));
    }
}