package org.apache.redis.jdbc;

import java.sql.*;

public class RedisJdbcUpdateExample {
    public static void main(String[] args) {
        String url = "jdbc:redis://localhost:6379/0";
        
        try {
            // Register Redis JDBC driver
            Class.forName("org.apache.redis.jdbc.RedisDriver");
            
            try (Connection conn = DriverManager.getConnection(url, new java.util.Properties())) {
                // Create a table
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE TABLE users (id VARCHAR(36), name VARCHAR(100), email VARCHAR(100))");
                }

                // Insert some test data
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO users (id, name, email) VALUES (?, ?, ?)")) {
                    
                    // First user
                    pstmt.setString(1, "1");
                    pstmt.setString(2, "John Doe");
                    pstmt.setString(3, "john@example.com");
                    pstmt.executeUpdate();

                    // Second user
                    pstmt.setString(1, "2");
                    pstmt.setString(2, "Jane Smith");
                    pstmt.setString(3, "jane@example.com");
                    pstmt.executeUpdate();
                }

                // Display initial data
                System.out.println("Initial data:");
                displayUsers(conn);

                // Update a user's email
                try (Statement stmt = conn.createStatement()) {
                    int updatedRows = stmt.executeUpdate(
                        "UPDATE users SET email='john.doe@example.com' WHERE name='John Doe'"
                    );
                    System.out.println("\nUpdated " + updatedRows + " row(s)");
                }

                // Display updated data
                System.out.println("\nAfter update:");
                displayUsers(conn);

                // Update multiple users using prepared statement
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE users SET email=? WHERE name=?")) {
                    
                    pstmt.setString(1, "jane.smith@example.com");
                    pstmt.setString(2, "Jane Smith");
                    int updatedRows = pstmt.executeUpdate();
                    System.out.println("\nUpdated " + updatedRows + " row(s) using prepared statement");
                }

                // Display final data
                System.out.println("\nFinal data:");
                displayUsers(conn);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void displayUsers(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            
            while (rs.next()) {
                System.out.printf("ID: %s, Name: %s, Email: %s%n",
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("email")
                );
            }
        }
    }
} 