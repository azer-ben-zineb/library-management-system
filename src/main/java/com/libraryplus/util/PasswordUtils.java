package com.libraryplus.util;

import org.mindrot.jbcrypt.BCrypt;
public final class PasswordUtils {
    private static final int WORKLOAD = 12;

    private PasswordUtils() {}

    public static String hashPassword(String plain) {
        String salt = BCrypt.gensalt(WORKLOAD);
        return BCrypt.hashpw(plain, salt);
    }

    public static boolean verifyPassword(String plain, String hashed) {
        if (plain == null || hashed == null) return false;
        return BCrypt.checkpw(plain, hashed);
    }
}

