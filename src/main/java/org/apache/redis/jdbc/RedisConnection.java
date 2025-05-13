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

import redis.clients.jedis.*;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisConnection implements java.sql.Connection {
    private static final Pattern STANDALONE_URL_PATTERN = Pattern.compile("jdbc:redis://([^:]+)(?::(\\d+))?(?:/([0-9]+))?");
    private static final Pattern CLUSTER_URL_PATTERN = Pattern.compile("jdbc:redis:cluster://([^/]+)(?:/([0-9]+))?");
    private static final Pattern SENTINEL_URL_PATTERN = Pattern.compile("jdbc:redis:sentinel://([^/]+)(?:/([0-9]+))?");

    private final Jedis jedis;
    private final boolean isCluster;
    private boolean isClosed = false;
    private boolean autoCommit = true;

    public RedisConnection(String url, Properties info) throws SQLException {
        if (url.startsWith("jdbc:redis:cluster://")) {
            this.jedis = createClusterConnection(url, info);
            this.isCluster = true;
        } else if (url.startsWith("jdbc:redis:sentinel://")) {
            this.jedis = createSentinelConnection(url, info);
            this.isCluster = false;
        } else {
            this.jedis = createStandaloneConnection(url, info);
            this.isCluster = false;
        }
    }

    private Jedis createStandaloneConnection(String url, Properties info) throws SQLException {
        Matcher matcher = STANDALONE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new SQLException("Invalid Redis URL format. Expected: jdbc:redis://host[:port][/database]");
        }

        String host = matcher.group(1);
        String portStr = matcher.group(2);
        String dbStr = matcher.group(3);

        int port = portStr != null ? Integer.parseInt(portStr) : 6379;
        int database = dbStr != null ? Integer.parseInt(dbStr) : 0;

        Jedis jedis = new Jedis(host, port);
        String password = info.getProperty("password");
        if (password != null && !password.trim().isEmpty()) {
            jedis.auth(password);
        }
        if (database > 0) {
            jedis.select(database);
        }
        return jedis;
    }

    private Jedis createClusterConnection(String url, Properties info) throws SQLException {
        Matcher matcher = CLUSTER_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new SQLException("Invalid Redis Cluster URL format. Expected: jdbc:redis:cluster://host1:port1,host2:port2,...");
        }

        String[] nodes = matcher.group(1).split(",");
        String[] firstNode = nodes[0].split(":");
        String host = firstNode[0];
        int port = Integer.parseInt(firstNode[1]);

        Jedis jedis = new Jedis(host, port);
        String password = info.getProperty("password");
        if (password != null && !password.trim().isEmpty()) {
            jedis.auth(password);
        }
        return jedis;
    }

    private Jedis createSentinelConnection(String url, Properties info) throws SQLException {
        Matcher matcher = SENTINEL_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new SQLException("Invalid Redis Sentinel URL format. Expected: jdbc:redis:sentinel://host1:port1,host2:port2,.../database");
        }

        String[] sentinels = matcher.group(1).split(",");
        String[] firstSentinel = sentinels[0].split(":");
        String host = firstSentinel[0];
        int port = Integer.parseInt(firstSentinel[1]);

        String dbStr = matcher.group(2);
        int database = dbStr != null ? Integer.parseInt(dbStr) : 0;

        Jedis jedis = new Jedis(host, port);
        String password = info.getProperty("password");
        if (password != null && !password.trim().isEmpty()) {
            jedis.auth(password);
        }
        if (database > 0) {
            jedis.select(database);
        }
        return jedis;
    }

    public Jedis getJedis() {
        return jedis;
    }

    private void checkClosed() throws SQLException {
        if (isClosed) {
            throw new SQLException("Connection is closed");
        }
    }

    @Override
    public void close() throws SQLException {
        if (!isClosed) {
            jedis.close();
            isClosed = true;
        }
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        close();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        // No-op as Jedis handles timeouts through its configuration
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkClosed();
        return new RedisStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkClosed();
        return new RedisPreparedStatement(this, sql);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }

    // Unsupported operations
    @Override
    public void commit() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void rollback() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return sql;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        // No-op
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        // No-op
    }

    @Override
    public String getCatalog() throws SQLException {
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return java.sql.Connection.TRANSACTION_NONE;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        // No-op
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (isClosed) {
            return false;
        }
        try {
            jedis.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new SQLClientInfoException();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new SQLClientInfoException();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return new Properties();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        // No-op
    }

    @Override
    public String getSchema() throws SQLException {
        return null;
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 2000;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
} 