package chat.server;
import java.util.*;
import java.util.concurrent.*;

class UserDatabase {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    public synchronized boolean registrieren(String username, String passwort) {
        if (users.containsKey(username)) return false;
        users.put(username, passwort);
        return true;
    }

    public boolean anmelden(String username, String passwort) {
        return passwort.equals(users.get(username));
    }
}