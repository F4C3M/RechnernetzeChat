package chat.server;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class UserDatabase {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    // kommt von ClientHandler.java/regestrierungBearbeiten() || hasht passwort
    public synchronized boolean registrieren(String username, String passwort) {
        if (users.containsKey(username)) {
            return false;
        }

        // ruft die hashPasswort Methode auf
        String hash = hashPasswort(passwort);
        // kommt aus dem hashPasswort() mit Hash
        users.put(username, hash);
        return true;
    }

    // kommt von ClientHandler.java/anmeldungBearbeiten()
    public boolean anmelden(String username, String passwort) {
        // überprüft, ob in der user Liste der Name existiert
        String gespeicherterHash = users.get(username);
        if (gespeicherterHash == null) {
            return false;
        }

        // eingetipptes Passwort wird gehasht (zum späteren Vergleich)
        String eingegebenerHash = hashPasswort(passwort);
        // vergleicht eingegebenes Passwort mit gespeichertem Passwort/Hash
        return gespeicherterHash.equals(eingegebenerHash);
    }

    // hasht die Passwörteter
    private String hashPasswort(String passwort) {
        try {
            // errechnet aus dem Passwort ein 256 bit Fingerabdruck
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(passwort.getBytes());

            // byte Hash in lesbaren hexString umwandeln
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