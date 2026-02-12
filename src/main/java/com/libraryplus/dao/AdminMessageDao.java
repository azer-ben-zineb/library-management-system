package com.libraryplus.dao;

import com.libraryplus.model.AdminMessage;

import java.util.List;

public interface AdminMessageDao {
    void create(AdminMessage message);

    List<AdminMessage> findAll();

    List<AdminMessage> findByStatus(String status);

    void markAsRead(int id);

    void saveAdminReply(int id, String reply, String newStatus);
}
