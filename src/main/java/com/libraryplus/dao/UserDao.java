package com.libraryplus.dao;

import com.libraryplus.model.User;

import java.util.Optional;

public interface UserDao {
    Optional<User> findByEmail(String email) throws Exception;

    int createUser(User user) throws Exception;

    Optional<User> findById(int id) throws Exception;

    void updateUser(User user) throws Exception;

    java.util.List<User> findAll() throws Exception;
}
