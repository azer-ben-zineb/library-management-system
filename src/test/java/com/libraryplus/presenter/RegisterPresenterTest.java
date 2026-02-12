package com.libraryplus.presenter;

import com.libraryplus.dao.UserDao;
import com.libraryplus.model.User;
import com.libraryplus.util.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

 
@DisplayName("RegisterPresenter Tests")
public class RegisterPresenterTest {

    private RegisterPresenter presenter;
    private UserDao mockUserDao;

    @BeforeEach
    public void setUp() {
        mockUserDao = mock(UserDao.class);
        presenter = new RegisterPresenter(mockUserDao);
    }

    @Test
    @DisplayName("Should register a new client successfully")
    public void testRegisterSuccess() throws Exception {
        String email = "user@example.com";
        String password = "SecurePass123";
        String fullName = "John Doe";
        String phone = "+216 20 123 456";
        LocalDate dob = LocalDate.of(1990, 1, 15);
        String cardNumber = "1234567890123456";
        Double balance = 100.0;

        
        when(mockUserDao.findByEmail(email)).thenReturn(java.util.Optional.empty());

        
        when(mockUserDao.createUser(any())).thenReturn(1);

        String result = presenter.register(email, password, fullName, phone, dob, cardNumber, balance);

        assertTrue(result.contains("successful"), "Result should indicate success");
        verify(mockUserDao).findByEmail(email);
        verify(mockUserDao).createUser(any());
    }

    @Test
    @DisplayName("Should reject empty email")
    public void testRegisterEmptyEmail() {
        String result = presenter.register("", "SecurePass123", "John Doe", "+216 20 123 456", LocalDate.now(), "1234567890123456", 100.0);
        assertTrue(result.contains("Email is required"));
    }

    @Test
    @DisplayName("Should reject invalid email format")
    public void testRegisterInvalidEmail() {
        String result = presenter.register("notanemail", "SecurePass123", "John Doe", "+216 20 123 456", LocalDate.now(), "1234567890123456", 100.0);
        assertTrue(result.contains("Invalid email format"));
    }

    @Test
    @DisplayName("Should reject short password")
    public void testRegisterShortPassword() {
        String result = presenter.register("user@example.com", "short", "John Doe", "+216 20 123 456", LocalDate.now(), "1234567890123456", 100.0);
        assertTrue(result.contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Should reject existing email")
    public void testRegisterDuplicateEmail() throws Exception {
        String email = "duplicate@example.com";
        User existingUser = new User();
        existingUser.setEmail(email);

        when(mockUserDao.findByEmail(email)).thenReturn(java.util.Optional.of(existingUser));

        String result = presenter.register(email, "SecurePass123", "Jane Doe", "+216 20 123 456", LocalDate.now(), "1234567890123456", 100.0);

        assertTrue(result.contains("already registered"));
        verify(mockUserDao).findByEmail(email);
        verify(mockUserDao, never()).createUser(any());
    }

    @Test
    @DisplayName("Should reject negative card balance")
    public void testRegisterNegativeBalance() {
        String result = presenter.register("user@example.com", "SecurePass123", "John Doe", "+216 20 123 456", LocalDate.now(), "1234567890123456", -50.0);
        assertTrue(result.contains("cannot be negative"));
    }
}

