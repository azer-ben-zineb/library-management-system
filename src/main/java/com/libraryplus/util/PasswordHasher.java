package com.libraryplus.util;

import org.mindrot.jbcrypt.BCrypt;

 
public final class PasswordHasher {

    
    private static final int WORK_FACTOR = 12;

    private PasswordHasher() {
        
    }

     
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        String salt = BCrypt.gensalt(WORK_FACTOR);
        return BCrypt.hashpw(rawPassword, salt);
    }

     
    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            
            return false;
        }
    }
}
