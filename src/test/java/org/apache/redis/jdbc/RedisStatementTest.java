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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class RedisStatementTest {
    private RedisConnection connection;
    private Statement statement;
    private static final String REDIS_URL = "jdbc:redis://localhost:6379/0";

    @BeforeEach
    void setUp() throws SQLException {
        Properties info = new Properties();
        connection = new RedisConnection(REDIS_URL, info);
        statement = connection.createStatement();
        
        // Create test table
        statement.execute("CREATE TABLE test_table (id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), value VARCHAR(255))");
    }

    @AfterEach
    void tearDown() throws SQLException {
        try {
            // Drop test table
            if (statement != null && !statement.isClosed()) {
                statement.execute("DROP TABLE IF EXISTS test_table");
                statement.close();
            }
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    @Test
    void testInsertAndSelect() throws SQLException {
        // Test INSERT
        int result = statement.executeUpdate("INSERT INTO test_table (name, value) VALUES ('test_name', 'test_value')");
        assertEquals(1, result);
        
        // Test SELECT
        ResultSet rs = statement.executeQuery("SELECT name, value FROM test_table WHERE name = 'test_name'");
        assertTrue(rs.next());
        assertEquals("test_name", rs.getString("name"));
        assertEquals("test_value", rs.getString("value"));
        assertFalse(rs.next());
    }

    @Test
    void testUpdate() throws SQLException {
        // Insert initial data
        statement.executeUpdate("INSERT INTO test_table (name, value) VALUES ('update_test', 'old_value')");
        
        // Test UPDATE
        int updateResult = statement.executeUpdate("UPDATE test_table SET value = 'new_value' WHERE name = 'update_test'");
        assertEquals(1, updateResult);

        // Verify the update
        ResultSet rs = statement.executeQuery("SELECT value FROM test_table WHERE name = 'update_test'");
        assertTrue(rs.next());
        assertEquals("new_value", rs.getString("value"));
    }

    @Test
    void testDelete() throws SQLException {
        // Insert data to delete
        statement.executeUpdate("INSERT INTO test_table (name, value) VALUES ('delete_test', 'to_be_deleted')");
        
        // Test DELETE
        int deleteResult = statement.executeUpdate("DELETE FROM test_table WHERE name = 'delete_test'");
        assertEquals(1, deleteResult);

        // Verify the deletion
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) as count FROM test_table WHERE name = 'delete_test'");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt("count"));
    }

    @Test
    void testClose() throws SQLException {
        statement.close();
        assertTrue(statement.isClosed());
        assertThrows(SQLException.class, () -> statement.executeQuery("SELECT * FROM test_table"));
    }

    @Test
    void testInvalidSQL() {
        assertThrows(SQLException.class, () -> statement.executeQuery("INVALID SQL COMMAND"));
    }

    @Test
    void testNullValues() throws SQLException {
        statement.executeUpdate("INSERT INTO test_table (name, value) VALUES ('null_test', NULL)");
        ResultSet rs = statement.executeQuery("SELECT value FROM test_table WHERE name = 'null_test'");
        assertTrue(rs.next());
        assertNull(rs.getString("value"));
    }

    @Test
    void testMultipleRows() throws SQLException {
        statement.executeUpdate("INSERT INTO test_table (name, value) VALUES ('row1', 'value1')");
        statement.executeUpdate("INSERT INTO test_table (name, value) VALUES ('row2', 'value2')");
        
        ResultSet rs = statement.executeQuery("SELECT name, value FROM test_table ORDER BY name");
        
        assertTrue(rs.next());
        assertEquals("row1", rs.getString("name"));
        assertEquals("value1", rs.getString("value"));
        
        assertTrue(rs.next());
        assertEquals("row2", rs.getString("name"));
        assertEquals("value2", rs.getString("value"));
        
        assertFalse(rs.next());
    }
} 