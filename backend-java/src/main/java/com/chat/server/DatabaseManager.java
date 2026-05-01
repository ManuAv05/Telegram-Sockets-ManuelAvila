package com.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.chat.model.Message;

public class DatabaseManager {
    
    private static final String DEFAULT_URL = "jdbc:sqlite:nexcord.db";
    
    public DatabaseManager() {
        setupTables();
    }
    
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DEFAULT_URL);
    }
    
    private void setupTables() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ");";
                
        String sqlMessages = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "channel TEXT NOT NULL," +
                "sender TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "timestamp LONG NOT NULL" +
                ");";
                
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlMessages);
            
            // Migraciones de fases posteriores
            try { stmt.execute("ALTER TABLE messages ADD COLUMN imageBase64 TEXT"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE messages ADD COLUMN messageId TEXT"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN avatarBase64 TEXT"); } catch (SQLException ignore) {}

        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
    
    public boolean authenticate(String username, String password, String providedAvatar) {
        String queryCheck = "SELECT password, avatarBase64 FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmtCheck = conn.prepareStatement(queryCheck)) {
            
            pstmtCheck.setString(1, username);
            ResultSet rs = pstmtCheck.executeQuery();
            
            if (rs.next()) {
                if(BCrypt.checkpw(password, rs.getString("password"))) {
                    // Update avatar if a new one is provided
                    if(providedAvatar != null && !providedAvatar.isEmpty()) {
                        String queryUpdate = "UPDATE users SET avatarBase64 = ? WHERE username = ?";
                        try(PreparedStatement pUpdate = conn.prepareStatement(queryUpdate)){
                            pUpdate.setString(1, providedAvatar);
                            pUpdate.setString(2, username);
                            pUpdate.executeUpdate();
                        }
                    }
                    return true;
                }
                return false;
            } else {
                String queryInsert = "INSERT INTO users(username, password, avatarBase64) VALUES(?, ?, ?)";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(queryInsert)) {
                    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                    pstmtInsert.setString(1, username);
                    pstmtInsert.setString(2, hashedPassword);
                    pstmtInsert.setString(3, providedAvatar);
                    pstmtInsert.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authing user: " + e.getMessage());
            return false;
        }
    }

    public Map<String, String> getAllAvatars() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT username, avatarBase64 FROM users WHERE avatarBase64 IS NOT NULL";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                map.put(rs.getString("username"), rs.getString("avatarBase64"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
    
    public void saveMessage(Message msg) {
        if (msg.getType().equals("MESSAGE")) {
            String sql = "INSERT INTO messages(channel, sender, content, timestamp, imageBase64, messageId) VALUES(?, ?, ?, ?, ?, ?)";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, msg.getChannel());
                pstmt.setString(2, msg.getUsername());
                pstmt.setString(3, msg.getContent());
                pstmt.setLong(4, msg.getTimestamp());
                pstmt.setString(5, msg.getImageBase64());
                pstmt.setString(6, msg.getMessageId());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error saving message: " + e.getMessage());
            }
        }
    }

    public boolean deleteMessage(String messageId, String sender) {
        String sql = "DELETE FROM messages WHERE messageId = ? AND sender = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, messageId);
            pstmt.setString(2, sender);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting message: " + e.getMessage());
            return false;
        }
    }
    
    public List<Message> getChannelHistory(String channel, int limit) {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT * FROM messages WHERE channel = ? ORDER BY timestamp DESC LIMIT ?) ORDER BY timestamp ASC";
        
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Message msg = new Message(
                    "MESSAGE",
                    rs.getString("sender"),
                    rs.getString("channel"),
                    rs.getString("content")
                );
                msg.setTimestamp(rs.getLong("timestamp"));
                try { msg.setImageBase64(rs.getString("imageBase64")); } catch (SQLException ignore) {}
                try { msg.setMessageId(rs.getString("messageId")); } catch (SQLException ignore) {}
                history.add(msg);
            }
        } catch (SQLException e) {
            System.err.println("Error getting history: " + e.getMessage());
        }
        
        return history;
    }
}
