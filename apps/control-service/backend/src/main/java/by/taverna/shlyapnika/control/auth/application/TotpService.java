package by.taverna.shlyapnika.control.auth.application;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Clock;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpService {
  private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;

  public TotpService() {
    this(Clock.systemUTC());
  }

  TotpService(Clock clock) {
    this.clock = clock;
  }

  public String generateSecret() {
    byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    return encodeBase32(bytes);
  }

  public boolean verify(String secret, String code) {
    if (code == null || !code.matches("\\d{6}")) return false;
    long currentStep = clock.instant().getEpochSecond() / 30;
    for (long step = currentStep - 1; step <= currentStep + 1; step++) {
      if (code.equals(generateCode(secret, step))) return true;
    }
    return false;
  }

  public String otpauthUrl(String issuer, String accountName, String secret) {
    return "otpauth://totp/" + url(issuer) + ":" + url(accountName) + "?secret=" + secret + "&issuer=" + url(issuer) + "&digits=6&period=30";
  }

  private static String generateCode(String secret, long step) {
    try {
      byte[] key = decodeBase32(secret);
      byte[] counter = ByteBuffer.allocate(8).putLong(step).array();
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(counter);
      int offset = hash[hash.length - 1] & 0x0f;
      int binary = ((hash[offset] & 0x7f) << 24)
          | ((hash[offset + 1] & 0xff) << 16)
          | ((hash[offset + 2] & 0xff) << 8)
          | (hash[offset + 3] & 0xff);
      return String.format("%06d", binary % 1_000_000);
    } catch (Exception error) {
      throw new IllegalStateException("TOTP generation failed", error);
    }
  }

  private static String encodeBase32(byte[] data) {
    StringBuilder output = new StringBuilder((data.length * 8 + 4) / 5);
    int buffer = 0;
    int bitsLeft = 0;
    for (byte item : data) {
      buffer = (buffer << 8) | (item & 0xff);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        output.append(BASE32[(buffer >> (bitsLeft - 5)) & 31]);
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) {
      output.append(BASE32[(buffer << (5 - bitsLeft)) & 31]);
    }
    return output.toString();
  }

  private static byte[] decodeBase32(String value) {
    int buffer = 0;
    int bitsLeft = 0;
    byte[] result = new byte[value.length() * 5 / 8];
    int index = 0;
    for (char raw : value.toUpperCase().toCharArray()) {
      int digit = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(raw);
      if (digit < 0) continue;
      buffer = (buffer << 5) | digit;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
        bitsLeft -= 8;
      }
    }
    return result;
  }

  private static String url(String value) {
    return value.replace(" ", "%20").replace("@", "%40");
  }
}
