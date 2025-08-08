package completo.projeto.completo.Autenticacao;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class CryptoUtil {

    // A chave deve ter exatamente 16 bytes (caracteres ASCII) para AES-128
    private static final String SECRET_KEY = "chave-supersegura"; // 16 caracteres

    public String encrypt(String value) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] original = cipher.doFinal(decoded);
            return new String(original);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar", e);
        }
    }
}
