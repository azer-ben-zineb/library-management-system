package com.libraryplus.util;

import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern DIGITS = Pattern.compile("^\\d+$");

    private ValidationUtils() {}

     
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        email = email.trim();
        if (email.isEmpty()) return false;
        return SIMPLE_EMAIL.matcher(email).matches();
    }

     
    public static boolean isValidCardNumber(String card) {
        if (card == null) return false;
        String s = card.trim();
        if (s.length() < 16) return false; 
        return DIGITS.matcher(s).matches();
    }

     
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        String s = phone.trim();
        if (s.length() < 8) return false;
        
        return s.matches("^[\\d+\\-().\\s]+$");
    }
}
