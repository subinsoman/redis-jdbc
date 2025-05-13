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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisDataException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.sql.Statement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisConnectionTest {
    private RedisConnection connection;
    private Properties info;

    // Test URLs for different deployment modes
    private static final String STANDALONE_URL = "jdbc:redis://localhost:6379/0";
    private static final String SENTINEL_URL = "jdbc:redis:sentinel://localhost:26379,localhost:26380/0";

    @BeforeEach
    void setUp() {
        info = new Properties();
        info.setProperty("timeout", "2000");
        info.setProperty("maxTotal", "10");
        info.setProperty("maxIdle", "5");
        info.setProperty("minIdle", "1");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Standalone Connection Creation")
    void testStandaloneConnection() throws SQLException {
        connection = new RedisConnection(STANDALONE_URL, info);
        assertNotNull(connection);
        assertTrue(connection.isValid(1));
        assertFalse(connection.isClosed());
    }

    @Test
    @Order(2)
    @DisplayName("Test Standalone Connection with Password")
    void testStandaloneConnectionWithPassword() throws SQLException {
        // First test with incorrect password
        Properties wrongPassInfo = new Properties(info);
        wrongPassInfo.setProperty("password", "wrong_password");
        
        assertThrows(JedisDataException.class, () -> {
            new RedisConnection(STANDALONE_URL, wrongPassInfo);
        }, "Should throw JedisDataException with wrong password");

        // Test with correct password
        Properties correctPassInfo = new Properties(info);
        //correctPassInfo.setProperty("password", "");  // Empty password for tests without auth
        
        connection = new RedisConnection(STANDALONE_URL, correctPassInfo);
        assertNotNull(connection);
        assertTrue(connection.isValid(1));
        assertFalse(connection.isClosed());

        // Test connection functionality
        try (Jedis jedis = connection.getJedis()) {
            assertNotNull(jedis);
            assertTrue(jedis.ping().equalsIgnoreCase("PONG"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test Sentinel Connection Creation")
    void testSentinelConnection() {
        Properties sentinelInfo = new Properties(info);
        sentinelInfo.setProperty("masterName", "mymaster");
        
        assertThrows(JedisConnectionException.class, () -> {
            connection = new RedisConnection(SENTINEL_URL, sentinelInfo);
        }, "Should throw exception when sentinel is not available");
    }

    @Test
    @Order(4)
    @DisplayName("Test Connection Properties")
    void testConnectionProperties() throws SQLException {
        connection = new RedisConnection(STANDALONE_URL, info);
        
        // Test default transaction isolation level
        assertEquals(Connection.TRANSACTION_NONE, connection.getTransactionIsolation());
        
        // Test auto-commit
        assertTrue(connection.getAutoCommit());
        connection.setAutoCommit(false);
        assertFalse(connection.getAutoCommit());
        
        // Test read-only mode
        assertFalse(connection.isReadOnly());
        connection.setReadOnly(true);
        assertFalse(connection.isReadOnly()); // Redis JDBC doesn't support read-only mode
    }

    @ParameterizedTest
    @Order(5)
    @ValueSource(strings = {
        "jdbc:redis://",
        "jdbc:redis://localhost:",
        "jdbc:redis://localhost:invalid",
        "jdbc:redis:invalid://localhost:6379",
        "jdbc:mysql://localhost:6379"
    })
    @DisplayName("Test Invalid URLs")
    void testInvalidUrls(String invalidUrl) {
        assertThrows(SQLException.class, () -> {
            new RedisConnection(invalidUrl, info);
        });
    }

    @Test
    @Order(6)
    @DisplayName("Test Statement Creation")
    void testStatementCreation() throws SQLException {
        connection = new RedisConnection(STANDALONE_URL, info);
        
        // Test regular statement
        assertNotNull(connection.createStatement());
        assertNotNull(connection.createStatement(
            java.sql.ResultSet.TYPE_FORWARD_ONLY,
            java.sql.ResultSet.CONCUR_READ_ONLY
        ));
        
        // Test prepared statement
        assertNotNull(connection.prepareStatement("SELECT * FROM test"));
        assertNotNull(connection.prepareStatement(
            "SELECT * FROM test",
            java.sql.ResultSet.TYPE_FORWARD_ONLY,
            java.sql.ResultSet.CONCUR_READ_ONLY
        ));
    }

    @Test
    @Order(7)
    @DisplayName("Test Connection Metadata")
    void testConnectionMetadata() throws SQLException {
        connection = new RedisConnection(STANDALONE_URL, info);
        
        // Test client info
        assertTrue(connection.getClientInfo().isEmpty());
        assertNull(connection.getClientInfo("any"));
        
        // Test catalog and schema
        assertNull(connection.getCatalog());
        assertNull(connection.getSchema());
        
        // Test network timeout
        assertEquals(2000, connection.getNetworkTimeout());
    }

    @Test
    @Order(8)
    @DisplayName("Test Connection Resource Management")
    void testResourceManagement() throws SQLException {
        connection = new RedisConnection(STANDALONE_URL, info);
        
        // Test Jedis instance
        try (Jedis jedis = connection.getJedis()) {
            assertNotNull(jedis);
            assertTrue(jedis.ping().equalsIgnoreCase("PONG"));
        }
        
        // Test connection validity
        assertTrue(connection.isValid(1));
        
        // Test connection close
        connection.close();
        assertTrue(connection.isClosed());
        assertThrows(SQLException.class, () -> connection.createStatement());
    }

    @Test
    @Order(9)
    @DisplayName("Test Unsupported Operations")
    void testUnsupportedOperations() throws SQLException {
        connection = new RedisConnection(STANDALONE_URL, info);
        
        assertThrows(SQLException.class, () -> connection.prepareCall(""));
        assertThrows(SQLException.class, () -> connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED));
        assertThrows(SQLException.class, () -> connection.setSavepoint());
        assertThrows(SQLException.class, () -> connection.rollback());
        assertThrows(SQLException.class, () -> connection.commit());
    }

    @Test
    @Order(10)
    @DisplayName("Test Connection Pool Configuration")
    void testConnectionPoolConfig() throws SQLException {
        Properties poolInfo = new Properties();
        poolInfo.setProperty("maxTotal", "20");
        poolInfo.setProperty("maxIdle", "10");
        poolInfo.setProperty("minIdle", "2");
        poolInfo.setProperty("timeout", "3000");
        
        connection = new RedisConnection(STANDALONE_URL, poolInfo);
        assertNotNull(connection);
        assertTrue(connection.isValid(1));
    }

    @Test
    @Order(11)
    @DisplayName("Test Multiple Database Selection")
    void testDatabaseSelection() throws SQLException {
        // Test default database (0)
        connection = new RedisConnection(STANDALONE_URL, info);
        assertNotNull(connection);
        
        // Test specific database
        connection = new RedisConnection("jdbc:redis://localhost:6379/1", info);
        assertNotNull(connection);
    }

    @Nested
    @DisplayName("Cluster Connection Tests")
    static class ClusterConnectionTests {
        private final int[] CLUSTER_PORTS = {7000, 7001, 7002};
        private Set<HostAndPort> clusterNodes;
        private String clusterUrl;
        private RedisConnection connection;
        private Properties info;

        @BeforeEach
        void setup() {
            // Initialize cluster nodes
            clusterNodes = new HashSet<>();
            StringBuilder urlBuilder = new StringBuilder("jdbc:redis:cluster://");
            
            for (int i = 0; i < CLUSTER_PORTS.length; i++) {
                clusterNodes.add(new HostAndPort("localhost", CLUSTER_PORTS[i]));
                urlBuilder.append("localhost:").append(CLUSTER_PORTS[i]);
                if (i < CLUSTER_PORTS.length - 1) {
                    urlBuilder.append(",");
                }
            }
            clusterUrl = urlBuilder.toString();
            
            // Initialize properties
            info = new Properties();
            info.setProperty("timeout", "2000");
            info.setProperty("maxTotal", "10");
            info.setProperty("maxIdle", "5");
            info.setProperty("minIdle", "1");
        }

        @AfterEach
        void tearDown() throws SQLException {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }

        @Test
        @DisplayName("Test Basic Cluster Connection")
        void testBasicClusterConnection() throws SQLException {
            Properties clusterInfo = new Properties(info);
            clusterInfo.setProperty("maxAttempts", "5");
            clusterInfo.setProperty("connectTimeout", "2000");
            clusterInfo.setProperty("soTimeout", "2000");
            
            try {
                connection = new RedisConnection(clusterUrl, clusterInfo);
                assertNotNull(connection);
                assertTrue(connection.isValid(1));
                
                // Test basic operations
                try (Jedis jedis = connection.getJedis()) {
                    assertNotNull(jedis);
                    assertEquals("PONG", jedis.ping());
                }
            } catch (JedisConnectionException e) {
                System.err.println("Failed to connect to Redis cluster: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Test Cluster Connection with Authentication")
        void testClusterConnectionWithAuth() {
            Properties clusterInfo = new Properties(info);
            clusterInfo.setProperty("password", "cluster_password");
            
            assertThrows(JedisConnectionException.class, () -> {
                new RedisConnection(clusterUrl, clusterInfo);
            }, "Should throw exception when authentication fails");
        }

        @Test
        @DisplayName("Test Cluster Connection Pool")
        void testClusterConnectionPool() throws SQLException {
            Properties clusterInfo = new Properties(info);
            clusterInfo.setProperty("maxTotal", "10");
            clusterInfo.setProperty("maxIdle", "5");
            clusterInfo.setProperty("minIdle", "1");
            clusterInfo.setProperty("maxWaitMillis", "2000");
            clusterInfo.setProperty("testOnBorrow", "true");
            
            try {
                connection = new RedisConnection(clusterUrl, clusterInfo);
                assertNotNull(connection);
                
                // Test multiple connections from pool
                for (int i = 0; i < 5; i++) {
                    try (Jedis jedis = connection.getJedis()) {
                        assertNotNull(jedis);
                        assertTrue(jedis.ping().equalsIgnoreCase("PONG"));
                    }
                }
            } catch (JedisConnectionException e) {
                System.err.println("Failed to test cluster connection pool: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Test Cluster Operations")
        void testClusterOperations() throws SQLException {
            Properties clusterInfo = new Properties(info);
            
            try {
                connection = new RedisConnection(clusterUrl, clusterInfo);
                
                // Test basic CRUD operations
                try (Statement stmt = connection.createStatement()) {
                    // Create table
                    stmt.execute("CREATE TABLE cluster_test (id INTEGER PRIMARY KEY, name VARCHAR(255))");
                    
                    // Insert data
                    stmt.executeUpdate("INSERT INTO cluster_test (id, name) VALUES (1, 'test1')");
                    stmt.executeUpdate("INSERT INTO cluster_test (id, name) VALUES (2, 'test2')");
                    
                    // Query data
                    ResultSet rs = stmt.executeQuery("SELECT * FROM cluster_test ORDER BY id");
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt("id"));
                    assertEquals("test1", rs.getString("name"));
                    
                    // Update data
                    stmt.executeUpdate("UPDATE cluster_test SET name = 'updated' WHERE id = 1");
                    
                    // Verify update
                    rs = stmt.executeQuery("SELECT name FROM cluster_test WHERE id = 1");
                    assertTrue(rs.next());
                    assertEquals("updated", rs.getString("name"));
                    
                    // Delete data
                    stmt.executeUpdate("DELETE FROM cluster_test WHERE id = 2");
                    
                    // Drop table
                    stmt.execute("DROP TABLE cluster_test");
                }
            } catch (SQLException | JedisConnectionException e) {
                System.err.println("Failed to test cluster operations: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Test Invalid Cluster URLs")
        void testInvalidClusterUrls() {
            String[] invalidUrls = {
                "jdbc:redis:cluster://",
                "jdbc:redis:cluster://localhost",
                "jdbc:redis:cluster://localhost:invalid",
                "jdbc:redis:cluster://localhost:7000,",
                "jdbc:redis:cluster://,localhost:7000"
            };
            
            for (String invalidUrl : invalidUrls) {
                assertThrows(SQLException.class, () -> {
                    new RedisConnection(invalidUrl, info);
                }, "Should throw exception for invalid URL: " + invalidUrl);
            }
        }
    }
} 