package com.libraryplus.presenter;

import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.User;
import com.libraryplus.util.PasswordUtils;
import com.libraryplus.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

 
public class RegisterPresenter {
    private static final Logger logger = LoggerFactory.getLogger(RegisterPresenter.class);
    private final UserDao userDao;

    public RegisterPresenter() {
        this.userDao = new UserDaoJdbc();
    }

    public RegisterPresenter(UserDao userDao) {
        this.userDao = userDao;
    }

     
    public String register(String email, String password, String fullName, String phone,
                           LocalDate dob, String cardNumber, Double cardBalance) {
        try {
            
            String validationError = validate(email, password, fullName, phone, cardNumber, cardBalance);
            if (validationError != null) {
                return validationError;
            }

            
            Optional<User> existing = userDao.findByEmail(email);
            if (existing.isPresent()) {
                return "Email already registered. Please use a different email or login.";
            }

            
            String passwordHash = PasswordUtils.hashPassword(password);

            
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setRoleId(2);  
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setDateOfBirth(dob);
            user.setCardNumber(cardNumber);  
            user.setCardBalance(cardBalance != null ? cardBalance : 0.0);

            
            int userId = userDao.createUser(user);
            if (userId <= 0) {
                logger.error("Failed to create user: userId={}", userId);
                return "Registration failed. Please try again later.";
            }

            logger.info("User registered successfully: email={}, userId={}", email, userId);
            return "Registration successful! You can now login.";

        } catch (Exception ex) {
            logger.error("Registration error", ex);
            return "An unexpected error occurred during registration: " + ex.getMessage();
        }
    }

     
    private String validate(String email, String password, String fullName, String phone,
                            String cardNumber, Double cardBalance) {
        if (email == null || email.isBlank()) {
            return "Email is required.";
        }
        if (!ValidationUtils.isValidEmail(email)) {
            return "Invalid email format.";
        }
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (fullName == null || fullName.isBlank()) {
            return "Full name is required.";
        }
        if (fullName.length() > 255) {
            return "Full name must be less than 255 characters.";
        }
        if (phone == null || phone.isBlank()) {
            return "Phone number is required.";
        }
        if (!ValidationUtils.isValidPhoneNumber(phone)) {
            return "Invalid phone number format.";
        }
        if (cardBalance != null && cardBalance < 0) {
            return "Card balance cannot be negative.";
        }
        return null;
    }
}

