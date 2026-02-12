package com.libraryplus.dao;

import com.libraryplus.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientDao {
    Optional<Client> findByUserId(int userId) throws Exception;

    int createClient(Client client) throws Exception;

    Optional<Client> findById(int id) throws Exception;

    List<Client> findAll() throws Exception;
}
