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
    private static final String REDIS_URL = "jdbc:redis://localhost:6379/0";

    @BeforeEach
    void setUp() throws SQLException {
        info = new Properties();
        // Don't set password for local testing
        connection = new RedisConnection(REDIS_URL, info);
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

    @Test
    void testClusterUrl() throws SQLException {
        String clusterUrl = "jdbc:redis:cluster://localhost:6379";
        RedisConnection clusterConnection = null;
        try {
            clusterConnection = new RedisConnection(clusterUrl, info);
            assertNotNull(clusterConnection);
        } finally {
            if (clusterConnection != null && !clusterConnection.isClosed()) {
                clusterConnection.close();
            }
        }
    }

    @Test
    void testSentinelUrl() throws SQLException {
        String sentinelUrl = "jdbc:redis:sentinel://localhost:26379/0";
        RedisConnection sentinelConnection = null;
        try {
            sentinelConnection = new RedisConnection(sentinelUrl, info);
            assertNotNull(sentinelConnection);
        } finally {
            if (sentinelConnection != null && !sentinelConnection.isClosed()) {
                sentinelConnection.close();
            }
        }
    }
} 