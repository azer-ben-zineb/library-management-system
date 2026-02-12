package com.libraryplus.dao;

import com.libraryplus.model.Comment;
import java.util.List;

public interface CommentDao {
    int createComment(Comment c) throws Exception;
    List<Comment> findByBook(String isbn) throws Exception;
}

