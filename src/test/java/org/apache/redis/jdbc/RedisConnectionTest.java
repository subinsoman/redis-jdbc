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
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class RedisConnectionTest {
    private RedisConnection connection;
    private Properties info;

    @BeforeEach
    void setUp() throws SQLException {
        info = new Properties();
        info.setProperty("password", ""); // Set Redis password if needed
        connection = new RedisConnection("jdbc:redis://localhost:6379/0", info);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void testCreateStatement() throws SQLException {
        assertNotNull(connection.createStatement());
    }

    @Test
    void testPrepareStatement() throws SQLException {
        assertNotNull(connection.prepareStatement("SELECT * FROM test"));
    }

    @Test
    void testGetJedis() throws SQLException {
        try (Jedis jedis = connection.getJedis()) {
            assertNotNull(jedis);
            assertTrue(jedis.ping().equalsIgnoreCase("PONG"));
        }
    }

    @Test
    void testClose() throws SQLException {
        connection.close();
        assertTrue(connection.isClosed());
    }

    @Test
    void testAutoCommit() throws SQLException {
        connection.setAutoCommit(false);
        assertFalse(connection.getAutoCommit());
        connection.setAutoCommit(true);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void testInvalidUrl() {
        assertThrows(SQLException.class, () -> {
            new RedisConnection("jdbc:redis:invalid", info);
        });
    }
} 