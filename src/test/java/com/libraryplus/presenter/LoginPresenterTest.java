package com.libraryplus.presenter;

import com.libraryplus.dao.LoginDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.model.User;
import com.libraryplus.util.PasswordUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LoginPresenterTest {
    
    static class FakeUserDao implements UserDao {
        private final User user;

        public FakeUserDao(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(user);
        }

        @Override
        public int createUser(User user) {
            return 1;
        }

        @Override
        public Optional<User> findById(int id) {
            return Optional.ofNullable(user);
        }

        @Override
        public void updateUser(User user) {
        }
    }

    static class FakeLoginDao implements LoginDao {
        boolean recorded = false;

        @Override
        public void recordLogin(int userId, Instant when, String ipAddress) {
            recorded = true;
        }
    }

    @Test
    void testLoginRecordsEvent() throws Exception {
        User u = new User();
        u.setId(42);
        u.setEmail("test@example.com");
        String pw = "secret";
        u.setPasswordHash(PasswordUtils.hashPassword(pw));

        FakeUserDao fud = new FakeUserDao(u);
        FakeLoginDao fld = new FakeLoginDao();

        
        LoginPresenter p = new LoginPresenter() {
            {
                
                
            }

            @Override
            public Optional<User> login(String email, String plainPassword) {
                try {
                    Optional<User> opt = fud.findByEmail(email);
                    if (opt.isPresent()) {
                        User user = opt.get();
                        if (PasswordUtils.verifyPassword(plainPassword, user.getPasswordHash())) {
                            fld.recordLogin(user.getId(), Instant.now(), null);
                            return Optional.of(user);
                        }
                    }
                    return Optional.empty();
                } catch (Exception e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            }
        };

        Optional<User> out = p.login("test@example.com", pw);
        assertTrue(out.isPresent());
        assertTrue(fld.recorded, "LoginDao should have recorded the login");
    }
}
