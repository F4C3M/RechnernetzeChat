package chat.client;
import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAManager {
    private KeyPair keyPair;

    public RSAManager() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048); // Sicherer Standard
            this.keyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String getPublicAsBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public static PublicKey getPublicFromBase64(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public String encrypt(String text, PublicKey partnerKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, partnerKey);
        byte[] bytes = cipher.doFinal(text.getBytes());
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String decrypt(String base64) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new String(cipher.doFinal(bytes));
    }
}