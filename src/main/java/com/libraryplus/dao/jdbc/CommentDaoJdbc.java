package com.libraryplus.dao.jdbc;
import com.libraryplus.dao.CommentDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Comment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CommentDaoJdbc implements CommentDao {
    private final DataSource ds;

    public CommentDaoJdbc() { this.ds = DataSourceConfig.getDataSource(); }

    @Override
    public int createComment(Comment c) throws Exception {
        String sql = "INSERT INTO comments (book_isbn, client_id, comment) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getBookIsbn());
            ps.setInt(2, c.getClientId());
            ps.setString(3, c.getComment());
            int affected = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return affected;
        }
    }

    @Override
    public List<Comment> findByBook(String isbn) throws Exception {
        String sql = "SELECT id, book_isbn, client_id, comment, created_at FROM comments WHERE book_isbn = ? ORDER BY created_at DESC";
        List<Comment> out = new ArrayList<>();
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Comment c = new Comment();
                    c.setId(rs.getInt("id"));
                    c.setBookIsbn(rs.getString("book_isbn"));
                    c.setClientId(rs.getInt("client_id"));
                    c.setComment(rs.getString("comment"));
                    out.add(c);
                }
            }
        }
        return out;
    }
}

