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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class RedisPreparedStatementTest {
    private RedisConnection connection;
    private static final String REDIS_URL = "jdbc:redis://localhost:6379/0";

    @BeforeEach
    void setUp() throws SQLException {
        Properties info = new Properties();
        connection = new RedisConnection(REDIS_URL, info);
        
        // Create test table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE test_table (id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), value VARCHAR(255))");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Drop test table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_table");
        }
        
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void testInsertString() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            stmt.setString(1, "test_name");
            stmt.setString(2, "test_value");
            int result = stmt.executeUpdate();
            assertEquals(1, result);

            // Verify the insertion
            try (PreparedStatement selectStmt = connection.prepareStatement("SELECT name, value FROM test_table WHERE name = ?")) {
                selectStmt.setString(1, "test_name");
                ResultSet rs = selectStmt.executeQuery();
                assertTrue(rs.next());
                assertEquals("test_name", rs.getString("name"));
                assertEquals("test_value", rs.getString("value"));
            }
        }
    }

    @Test
    void testInsertInteger() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            stmt.setString(1, "int_test");
            stmt.setInt(2, 42);
            int result = stmt.executeUpdate();
            assertEquals(1, result);

            // Verify the insertion
            try (PreparedStatement selectStmt = connection.prepareStatement("SELECT value FROM test_table WHERE name = ?")) {
                selectStmt.setString(1, "int_test");
                ResultSet rs = selectStmt.executeQuery();
                assertTrue(rs.next());
                assertEquals("42", rs.getString("value"));
            }
        }
    }

    @Test
    void testInsertLong() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            stmt.setString(1, "long_test");
            stmt.setLong(2, 9223372036854775807L);
            int result = stmt.executeUpdate();
            assertEquals(1, result);

            // Verify the insertion
            try (PreparedStatement selectStmt = connection.prepareStatement("SELECT value FROM test_table WHERE name = ?")) {
                selectStmt.setString(1, "long_test");
                ResultSet rs = selectStmt.executeQuery();
                assertTrue(rs.next());
                assertEquals("9223372036854775807", rs.getString("value"));
            }
        }
    }

    @Test
    void testInsertDouble() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            stmt.setString(1, "double_test");
            stmt.setDouble(2, 3.14159);
            int result = stmt.executeUpdate();
            assertEquals(1, result);

            // Verify the insertion
            try (PreparedStatement selectStmt = connection.prepareStatement("SELECT value FROM test_table WHERE name = ?")) {
                selectStmt.setString(1, "double_test");
                ResultSet rs = selectStmt.executeQuery();
                assertTrue(rs.next());
                assertEquals("3.14159", rs.getString("value"));
            }
        }
    }

    @Test
    void testInsertNull() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            stmt.setString(1, "null_test");
            stmt.setNull(2, java.sql.Types.VARCHAR);
            int result = stmt.executeUpdate();
            assertEquals(1, result);

            // Verify the insertion
            try (PreparedStatement selectStmt = connection.prepareStatement("SELECT value FROM test_table WHERE name = ?")) {
                selectStmt.setString(1, "null_test");
                ResultSet rs = selectStmt.executeQuery();
                assertTrue(rs.next());
                assertNull(rs.getString("value"));
            }
        }
    }

    @Test
    void testUpdate() throws SQLException {
        // Insert initial data
        try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            insertStmt.setString(1, "update_test");
            insertStmt.setString(2, "old_value");
            insertStmt.executeUpdate();
        }

        // Test UPDATE
        try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE test_table SET value = ? WHERE name = ?")) {
            updateStmt.setString(1, "new_value");
            updateStmt.setString(2, "update_test");
            int result = updateStmt.executeUpdate();
            assertEquals(1, result);

            // Verify the update
            try (PreparedStatement selectStmt = connection.prepareStatement("SELECT value FROM test_table WHERE name = ?")) {
                selectStmt.setString(1, "update_test");
                ResultSet rs = selectStmt.executeQuery();
                assertTrue(rs.next());
                assertEquals("new_value", rs.getString("value"));
            }
        }
    }

    @Test
    void testInvalidParameterIndex() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            assertThrows(SQLException.class, () -> stmt.setString(0, "invalid"));
            assertThrows(SQLException.class, () -> stmt.setString(3, "invalid"));
        }
    }

    @Test
    void testExecuteWithoutSettingParameters() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)")) {
            assertThrows(SQLException.class, stmt::executeUpdate);
        }
    }

    @Test
    void testClose() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO test_table (name, value) VALUES (?, ?)");
        stmt.close();
        assertTrue(stmt.isClosed());
        assertThrows(SQLException.class, () -> stmt.setString(1, "key"));
    }
} 