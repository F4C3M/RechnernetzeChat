package chat.client;
import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAManager {
    private KeyPair keyPaar;

    // hier ist auch Key erstellen
    public RSAManager() {
        // generator würfelt 2 Primzahlen zusammen Ergebnist = public + private Key
        // 2048 ist die Schlüssellänge ("goldener Standard")
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPaar = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // verwandelt deinen Key in ein "Salat"
    public String publicKeyEncoder() {
        return Base64.getEncoder().encodeToString(keyPaar.getPublic().getEncoded());
    }

    // verwandelt den "Salat" deines Freundes wieder in ein Schlüsselobjekt (zum rechnen)
    public static PublicKey publicKeyDecoder(String publicKeyEncoded) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKeyEncoded);
        
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    // verwandelt Nachricht mit dem publicKey vom Partener in "Salat"
    public String encrypt(String nachricht, PublicKey partnerKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, partnerKey);
        byte[] bytes = cipher.doFinal(nachricht.getBytes());
        
        return Base64.getEncoder().encodeToString(bytes);
    }

    // verwandelt "Salat" mit eigenem private Key wieder in eine Nachricht
    public String decrypt(String nachrichtEncoded) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPaar.getPrivate());
        byte[] bytes = Base64.getDecoder().decode(nachrichtEncoded);
        
        return new String(cipher.doFinal(bytes));
    }
}