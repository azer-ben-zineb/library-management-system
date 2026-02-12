package com.libraryplus.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

 
public final class EncryptionUtils {
    private static final String ENV_KEY_NAME = "CARD_ENC_KEY";
    private static final int GCM_TAG_LENGTH = 128; 
    private static final int IV_LENGTH = 12; 
    private static final SecretKey KEY;
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        SecretKey k = null;
        try {
            String b64 = System.getenv(ENV_KEY_NAME);
            if (b64 != null && !b64.isBlank()) {
                byte[] raw = Base64.getDecoder().decode(b64.trim());
                k = new SecretKeySpec(raw, "AES");
            }
        } catch (Exception e) {
            
        }
        if (k == null) {
            try {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                k = kg.generateKey();
                System.err.println("Warning: CARD_ENC_KEY not set; using ephemeral encryption key (not persistent). Set env var CARD_ENC_KEY=base64(key) for persistent encryption.");
            } catch (Exception e) {
                
                try {
                    KeyGenerator kg = KeyGenerator.getInstance("AES");
                    kg.init(128);
                    k = kg.generateKey();
                } catch (Exception ex) {
                    k = null;
                }
            }
        }
        KEY = k;
    }

    private EncryptionUtils() {}

    public static String encrypt(String plaintext) {
        if (plaintext == null) return null;
        
        if (plaintext.startsWith("ENC:")) return plaintext;
        if (KEY == null) return plaintext; 
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, spec);
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.allocate(iv.length + cipherBytes.length);
            bb.put(iv);
            bb.put(cipherBytes);
            return "ENC:" + Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            
            System.err.println("Card encryption failed: " + e.getMessage());
            return plaintext;
        }
    }

    public static String decrypt(String cipherB64) {
        if (cipherB64 == null) return null;
        
        if (!cipherB64.startsWith("ENC:")) return cipherB64;
        if (KEY == null) return cipherB64; 
        try {
            String b64 = cipherB64.substring(4);
            byte[] all = Base64.getDecoder().decode(b64);
            ByteBuffer bb = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH];
            bb.get(iv);
            byte[] cipherBytes = new byte[bb.remaining()];
            bb.get(cipherBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, KEY, spec);
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            
            return cipherB64;
        }
    }
}
