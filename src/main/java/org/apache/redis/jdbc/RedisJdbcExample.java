/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.redis.jdbc;

import java.sql.*;

public class RedisJdbcExample {
    public static void main(String[] args) {
        // Register the driver (this is done automatically by the static block in RedisDriver)
        try {
            Class.forName("org.apache.redis.jdbc.RedisDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load Redis JDBC driver");
            e.printStackTrace();
            return;
        }

        // Connection parameters
        String url = "jdbc:redis://localhost:6379";
        String username = "default";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("Connected to Redis!");

            // Create a table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "email TEXT NOT NULL" +
                    ")"
                );
                System.out.println("Created users table");
            }

            // Insert data using PreparedStatement
            String insertSql = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                // Insert first user
                pstmt.setInt(1, 1);
                pstmt.setString(2, "John Doe");
                pstmt.setString(3, "john@example.com");
                pstmt.executeUpdate();

                // Insert second user
                pstmt.setInt(1, 2);
                pstmt.setString(2, "Jane Smith");
                pstmt.setString(3, "jane@example.com");
                pstmt.executeUpdate();

                System.out.println("Inserted test data");
            }

            // Query data
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                
                System.out.println("\nUsers in database:");
                System.out.println("ID\tName\t\tEmail");
                System.out.println("--------------------------------");

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    System.out.printf("%d\t%s\t%s%n", id, name, email);
                }
            }

            // Query with WHERE clause
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = 1")) {
                
                System.out.println("\nUser with ID 1:");
                if (rs.next()) {
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    System.out.printf("Name: %s, Email: %s%n", name, email);
                }
            }

            // Delete data
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate("DELETE FROM users WHERE id = 2");
                System.out.printf("\nDeleted %d user(s)%n", deleted);
            }

        } catch (SQLException e) {
            System.err.println("Database error occurred");
            e.printStackTrace();
        }
    }
} 