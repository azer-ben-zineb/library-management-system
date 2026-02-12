package com.libraryplus.presenter;

import com.libraryplus.dao.LoginDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.LoginDaoJdbc;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.User;
import com.libraryplus.util.PasswordUtils;

import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
public class LoginPresenter {
    private final UserDao userDao;
    private final LoginDao loginDao;

    private static final Logger logger = LoggerFactory.getLogger(LoginPresenter.class);

    public LoginPresenter() {
        this.userDao = new UserDaoJdbc();
        this.loginDao = new LoginDaoJdbc();
    }

    public Optional<User> login(String email, String plainPassword) {
        try {
            Optional<User> u = userDao.findByEmail(email);
            if (u.isPresent()) {
                User user = u.get();
                if (PasswordUtils.verifyPassword(plainPassword, user.getPasswordHash())) {
                    
                    try {
                        loginDao.recordLogin(user.getId(), Instant.now(), null);
                    } catch (Exception ex) {
                        
                        logger.warn("Failed recording login event for user {}", user.getEmail(), ex);
                    }
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error during login", e);
            return Optional.empty();
        }
    }

    public boolean register(User newUser, String plainPassword) {
        try {
            String hashed = PasswordUtils.hashPassword(plainPassword);
            newUser.setPasswordHash(hashed);
            
            newUser.setRoleId(2);
            int created = userDao.createUser(newUser);
            return created > 0;
        } catch (Exception e) {
            logger.error("Registration failed", e);
            return false;
        }
    }
}
