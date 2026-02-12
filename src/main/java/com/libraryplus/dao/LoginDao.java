package com.libraryplus.dao;

import java.time.Instant;

public interface LoginDao {
    void recordLogin(int userId, Instant when, String ipAddress) throws Exception;
}

