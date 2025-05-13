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

import redis.clients.jedis.Jedis;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisStatement implements Statement {
    protected final RedisConnection connection;
    protected boolean isClosed = false;
    protected int maxRows = 0;
    protected int queryTimeout = 0;

    public RedisStatement(RedisConnection connection) {
        this.connection = connection;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkClosed();
        try (Jedis jedis = connection.getJedis()) {
            SQLCommand command = parseSQLCommand(sql);
            List<Map<String, String>> results = executeRedisCommand(jedis, command);
            return new RedisResultSet(this, results);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        checkClosed();
        try (Jedis jedis = connection.getJedis()) {
            SQLCommand command = parseSQLCommand(sql);
            return executeRedisUpdate(jedis, command);
        }
    }

    protected SQLCommand parseSQLCommand(String sql) throws SQLException {
        sql = sql.trim().toLowerCase();
        
        if (sql.startsWith("create table")) {
            return parseCreateTable(sql);
        } else if (sql.startsWith("insert into")) {
            return parseInsert(sql);
        } else if (sql.startsWith("select")) {
            return parseSelect(sql);
        } else if (sql.startsWith("delete from")) {
            return parseDelete(sql);
        } else if (sql.startsWith("update")) {
            return parseUpdate(sql);
        } else {
            throw new SQLException("Unsupported SQL command: " + sql);
        }
    }

    protected SQLCommand parseCreateTable(String sql) throws SQLException {
        Pattern pattern = Pattern.compile("create table (?:if not exists )?([\\w]+)\\s*\\((.+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (!matcher.find()) {
            throw new SQLException("Invalid CREATE TABLE syntax");
        }

        String tableName = matcher.group(1);
        String columnsStr = matcher.group(2);
        
        return new SQLCommand(SQLCommand.Type.CREATE_TABLE, tableName, columnsStr);
    }

    protected SQLCommand parseInsert(String sql) throws SQLException {
        Pattern pattern = Pattern.compile("insert into ([\\w]+)\\s*\\((.+?)\\)\\s*values\\s*\\((.+?)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (!matcher.find()) {
            throw new SQLException("Invalid INSERT syntax");
        }

        String tableName = matcher.group(1);
        String columns = matcher.group(2);
        String values = matcher.group(3);
        
        return new SQLCommand(SQLCommand.Type.INSERT, tableName, columns, values);
    }

    protected SQLCommand parseSelect(String sql) throws SQLException {
        Pattern pattern = Pattern.compile("select (.+?) from ([\\w]+)(?: where (.+))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (!matcher.find()) {
            throw new SQLException("Invalid SELECT syntax");
        }

        String columns = matcher.group(1);
        String tableName = matcher.group(2);
        String whereClause = matcher.group(3);
        
        return new SQLCommand(SQLCommand.Type.SELECT, tableName, columns, whereClause);
    }

    protected SQLCommand parseDelete(String sql) throws SQLException {
        Pattern pattern = Pattern.compile("delete from ([\\w]+)(?: where (.+))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (!matcher.find()) {
            throw new SQLException("Invalid DELETE syntax");
        }

        String tableName = matcher.group(1);
        String whereClause = matcher.group(2);
        
        return new SQLCommand(SQLCommand.Type.DELETE, tableName, whereClause);
    }

    protected SQLCommand parseUpdate(String sql) throws SQLException {
        // Pattern for both string values and numeric/parameter values
        Pattern pattern = Pattern.compile("update ([\\w]+)\\s+set\\s+([\\w]+)\\s*=\\s*('[^']*'|\\d+|\\?)(?: where (.+))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (!matcher.find()) {
            throw new SQLException("Invalid UPDATE syntax");
        }

        String tableName = matcher.group(1);
        String columnName = matcher.group(2);
        String value = matcher.group(3);
        String whereClause = matcher.group(4);
        
        // Remove quotes if present
        if (value != null && value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        
        return new SQLCommand(SQLCommand.Type.UPDATE, tableName, columnName, value, whereClause);
    }

    protected List<Map<String, String>> executeRedisCommand(Jedis jedis, SQLCommand command) throws SQLException {
        switch (command.type) {
            case SELECT:
                return executeSelect(jedis, command);
            case CREATE_TABLE:
                executeCreateTable(jedis, command);
                return Collections.emptyList();
            default:
                throw new SQLException("Unsupported command type for query: " + command.type);
        }
    }

    protected int executeRedisUpdate(Jedis jedis, SQLCommand command) throws SQLException {
        switch (command.type) {
            case INSERT:
                return executeInsert(jedis, command);
            case DELETE:
                return executeDelete(jedis, command);
            case UPDATE:
                return executeUpdate(jedis, command);
            case CREATE_TABLE:
                executeCreateTable(jedis, command);
                return 0;
            default:
                throw new SQLException("Unsupported command type for update: " + command.type);
        }
    }

    protected void executeCreateTable(Jedis jedis, SQLCommand command) throws SQLException {
        // In Redis, we don't actually create tables, but we can store the schema
        String schemaKey = "schema:" + command.tableName;
        jedis.set(schemaKey, command.columns);
    }

    protected int executeInsert(Jedis jedis, SQLCommand command) throws SQLException {
        String[] columnNames = command.columns.split(",");
        String[] values = command.values.split(",");
        
        if (columnNames.length != values.length) {
            throw new SQLException("Column count doesn't match values count");
        }

        Map<String, String> hash = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            hash.put(columnNames[i].trim(), values[i].trim().replaceAll("'", ""));
        }

        String key = command.tableName + ":" + UUID.randomUUID().toString();
        jedis.hmset(key, hash);
        jedis.sadd(command.tableName + ":keys", key);
        
        return 1;
    }

    protected List<Map<String, String>> executeSelect(Jedis jedis, SQLCommand command) throws SQLException {
        Set<String> keys = jedis.smembers(command.tableName + ":keys");
        List<Map<String, String>> results = new ArrayList<>();
        
        for (String key : keys) {
            Map<String, String> hash = jedis.hgetAll(key);
            if (matchesWhereClause(hash, command.whereClause)) {
                results.add(hash);
            }
        }
        
        return results;
    }

    protected int executeDelete(Jedis jedis, SQLCommand command) throws SQLException {
        Set<String> keys = jedis.smembers(command.tableName + ":keys");
        int deletedCount = 0;
        
        for (String key : keys) {
            Map<String, String> hash = jedis.hgetAll(key);
            if (matchesWhereClause(hash, command.whereClause)) {
                jedis.del(key);
                jedis.srem(command.tableName + ":keys", key);
                deletedCount++;
            }
        }
        
        return deletedCount;
    }

    protected int executeUpdate(Jedis jedis, SQLCommand command) throws SQLException {
        Set<String> keys = jedis.smembers(command.tableName + ":keys");
        int updatedCount = 0;
        
        for (String key : keys) {
            Map<String, String> hash = jedis.hgetAll(key);
            if (matchesWhereClause(hash, command.whereClause)) {
                hash.put(command.columns, command.values);
                jedis.hmset(key, hash);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    protected boolean matchesWhereClause(Map<String, String> hash, String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            return true;
        }
        
        // Very basic WHERE clause parsing - only handles simple equality
        String[] parts = whereClause.split("=");
        if (parts.length != 2) {
            return true;
        }
        
        String column = parts[0].trim();
        String value = parts[1].trim().replaceAll("'", "");
        
        return hash.containsKey(column) && hash.get(column).equals(value);
    }

    protected void checkClosed() throws SQLException {
        if (isClosed) {
            throw new SQLException("Statement is closed");
        }
    }

    @Override
    public void close() throws SQLException {
        isClosed = true;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        checkClosed();
        try {
            executeQuery(sql);
            return true;
        } catch (SQLException e) {
            executeUpdate(sql);
            return false;
        }
    }

    // Required Statement interface methods with basic implementations
    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
    }

    @Override
    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        this.maxRows = max;
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.queryTimeout = seconds;
    }

    @Override
    public void cancel() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return -1;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw new SQLException("Only FETCH_FORWARD is supported");
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void clearBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return executeUpdate(sql);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return executeUpdate(sql);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return executeUpdate(sql);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return execute(sql);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    protected static class SQLCommand {
        enum Type {
            CREATE_TABLE,
            INSERT,
            SELECT,
            DELETE,
            UPDATE
        }

        final Type type;
        final String tableName;
        final String columns;
        final String values;
        final String whereClause;

        SQLCommand(Type type, String tableName, String columns) {
            this(type, tableName, columns, null, null);
        }

        SQLCommand(Type type, String tableName, String columns, String values) {
            this(type, tableName, columns, values, null);
        }

        SQLCommand(Type type, String tableName, String columns, String values, String whereClause) {
            this.type = type;
            this.tableName = tableName;
            this.columns = columns;
            this.values = values;
            this.whereClause = whereClause;
        }
    }
} 