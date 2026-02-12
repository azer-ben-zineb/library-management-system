package com.libraryplus.validation;

import com.libraryplus.util.ValidationUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilsTest {
    @Test
    public void testValidEmails() {
        assertTrue(ValidationUtils.isValidEmail("a@b.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name+tag@domain.co"));
    }

    @Test
    public void testInvalidEmails() {
        assertFalse(ValidationUtils.isValidEmail("useratexample"));
        assertFalse(ValidationUtils.isValidEmail("user@domain"));
        assertFalse(ValidationUtils.isValidEmail("user@domaincom"));
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
    }

    @Test
    public void testCardValid() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append('1');
        assertTrue(ValidationUtils.isValidCardNumber(sb.toString()));

        
        for (int i = 0; i < 20; i++) sb.append('1');
        assertTrue(ValidationUtils.isValidCardNumber(sb.toString()));
    }

    @Test
    public void testCardInvalid() {
        assertFalse(ValidationUtils.isValidCardNumber("123"));
        assertFalse(ValidationUtils.isValidCardNumber("a".repeat(16)));
        assertFalse(ValidationUtils.isValidCardNumber(null));
        String tooShort = "1".repeat(15);
        assertFalse(ValidationUtils.isValidCardNumber(tooShort));
    }
}
