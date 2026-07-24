package by.taverna.shlyapnika.control.auth.application;

import by.taverna.shlyapnika.control.config.ControlProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class SecretEncryptionService {
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public SecretEncryptionService(ControlProperties properties) {
    this.key = new SecretKeySpec(sha256(properties.encryptionKey()), "AES");
  }

  public String encrypt(String plainText) {
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
      buffer.put(iv);
      buffer.put(cipherText);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    } catch (Exception error) {
      throw new IllegalStateException("Secret encryption failed", error);
    }
  }

  public String decrypt(String encrypted) {
    try {
      byte[] payload = Base64.getUrlDecoder().decode(encrypted);
      ByteBuffer buffer = ByteBuffer.wrap(payload);
      byte[] iv = new byte[IV_BYTES];
      buffer.get(iv);
      byte[] cipherText = new byte[buffer.remaining()];
      buffer.get(cipherText);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    } catch (Exception error) {
      throw new IllegalArgumentException("Secret decryption failed", error);
    }
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }
}
