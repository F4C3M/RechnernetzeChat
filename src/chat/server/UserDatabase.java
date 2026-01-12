package chat.server;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class UserDatabase {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    // zum registrieren (neu angepasst für Hashing || Zeile 14-15)
    public synchronized boolean registrieren(String username, String passwort) {
        if (users.containsKey(username)) return false;

        String hash = hashPasswort(passwort);
        users.put(username, hash);
        return true;
    }

    // zum anmelden (neu angepasst für Hashing || Zeile 21-24)
    public boolean anmelden(String username, String passwort) {
        String gespeicherterHash = users.get(username);
        if (gespeicherterHash == null) return false;

        String eingegebenerHash = hashPasswort(passwort);
        return gespeicherterHash.equals(eingegebenerHash);
    }


    // für hashing der Passworte
    private String hashPasswort(String passwort) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(passwort.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 nicht verfügbar", e);
        }
    }

    // Kommetar zum Hashing:
    // „Passwörter werden mit einer kryptografischen Hashfunktion 
    // (SHA-256) gespeichert, sodass selbst bei einem Datenbankleck 
    // keine Klartext-Passwörter bekannt werden.“
}