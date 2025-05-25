package pl.openmc.bungee.auth.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for AES encryption and decryption
 */
public class AESUtil {
  // Constants
  private static final String AES = "AES";
  private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int T_LEN = 96;
  private static final int ENCRYPT_MODE = 1;
  private static final int DECRYPT_MODE = 2;
  private static final int AES_KEY_SIZE = 16; // 128 bits

  // Secure random for IV generation
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // Encryption key
  private final Key key;

  /**
   * Adjust a key to the correct length for AES
   *
   * @param key The key to adjust
   * @return The adjusted key bytes
   * @throws Exception If an error occurs
   */
  public static byte[] fixKeyLength(String key) throws Exception {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }

    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
    MessageDigest sha = MessageDigest.getInstance("SHA-256");
    keyBytes = sha.digest(keyBytes);
    keyBytes = Arrays.copyOf(keyBytes, AES_KEY_SIZE);
    return keyBytes;
  }

  /**
   * Create a new AES utility with the given key
   *
   * @param encodedKey The Base64-encoded key
   */
  public AESUtil(String encodedKey) {
    if (encodedKey == null || encodedKey.isEmpty()) {
      throw new IllegalArgumentException("Encoded key cannot be null or empty");
    }

    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
    this.key = new SecretKeySpec(decodedKey, AES);
  }

  /**
   * Encrypt a string using AES-GCM
   *
   * @param plaintext The text to encrypt
   * @return The Base64-encoded encrypted text
   * @throws NoSuchPaddingException If the padding scheme is not available
   * @throws NoSuchAlgorithmException If the algorithm is not available
   * @throws InvalidAlgorithmParameterException If the algorithm parameters are invalid
   * @throws InvalidKeyException If the key is invalid
   * @throws IllegalBlockSizeException If the block size is invalid
   * @throws BadPaddingException If the padding is invalid
   */
  public String encrypt(String plaintext) throws NoSuchPaddingException, NoSuchAlgorithmException,
      InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    if (plaintext == null || plaintext.isEmpty()) {
      return "";
    }

    // Generate a random IV
    byte[] iv = new byte[IV_LENGTH];
    SECURE_RANDOM.nextBytes(iv);

    // Initialize cipher
    Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
    GCMParameterSpec ivSpec = new GCMParameterSpec(T_LEN, iv);
    cipher.init(ENCRYPT_MODE, key, ivSpec);

    // Encrypt the text
    byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
    byte[] ciphertext = cipher.doFinal(plaintextBytes);

    // Combine IV and ciphertext
    byte[] encrypted = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, encrypted, 0, iv.length);
    System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

    // Encode as Base64
    return Base64.getEncoder().encodeToString(encrypted);
  }

  /**
   * Decrypt a string using AES-GCM
   *
   * @param encryptedText The Base64-encoded encrypted text
   * @return The decrypted text
   * @throws NoSuchPaddingException If the padding scheme is not available
   * @throws NoSuchAlgorithmException If the algorithm is not available
   * @throws InvalidAlgorithmParameterException If the algorithm parameters are invalid
   * @throws InvalidKeyException If the key is invalid
   * @throws IllegalBlockSizeException If the block size is invalid
   * @throws BadPaddingException If the padding is invalid
   */
  public String decrypt(String encryptedText) throws NoSuchPaddingException, NoSuchAlgorithmException,
      InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    if (encryptedText == null || encryptedText.isEmpty()) {
      return "";
    }

    // Decode from Base64
    byte[] decoded = Base64.getDecoder().decode(encryptedText);

    if (decoded.length < IV_LENGTH) {
      throw new IllegalArgumentException("Encrypted text is too short");
    }

    // Extract IV and ciphertext
    byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
    byte[] ciphertext = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);

    // Initialize cipher
    Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
    GCMParameterSpec ivSpec = new GCMParameterSpec(T_LEN, iv);
    cipher.init(DECRYPT_MODE, key, ivSpec);

    // Decrypt the text
    byte[] decryptedBytes = cipher.doFinal(ciphertext);

    // Convert to string
    return new String(decryptedBytes, StandardCharsets.UTF_8);
  }
}
