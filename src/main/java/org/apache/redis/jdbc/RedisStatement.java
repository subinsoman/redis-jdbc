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
import redis.clients.jedis.Transaction;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisStatement implements Statement {
    protected final RedisConnection connection;
    protected boolean isClosed = false;
    protected int maxRows = 0;
    protected int queryTimeout = 0;
    protected ResultSet currentResultSet;
    protected int currentUpdateCount = -1;

    public RedisStatement(RedisConnection connection) {
        this.connection = connection;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkClosed();
        try (Jedis jedis = connection.getJedis()) {
            SQLCommand command = parseSQLCommand(sql);
            if (command.type != SQLCommand.Type.SELECT) {
                throw new SQLException("SQL command must be a SELECT statement");
            }
            List<Map<String, String>> results = executeRedisCommand(jedis, command);
            currentResultSet = new RedisResultSet(this, results);
            return currentResultSet;
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        checkClosed();
        try (Jedis jedis = connection.getJedis()) {
            SQLCommand command = parseSQLCommand(sql);
            if (command.type == SQLCommand.Type.SELECT) {
                throw new SQLException("SQL command cannot be a SELECT statement");
            }
            currentUpdateCount = executeRedisUpdate(jedis, command);
            return currentUpdateCount;
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
        } else if (sql.startsWith("drop table")) {
            return parseDropTable(sql);
        } else {
            throw new SQLException("Unsupported SQL command: " + sql);
        }
    }

    protected SQLCommand parseDropTable(String sql) throws SQLException {
        Pattern pattern = Pattern.compile("drop table (?:if exists )?([\\w]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (!matcher.find()) {
            throw new SQLException("Invalid DROP TABLE syntax");
        }

        String tableName = matcher.group(1);
        return new SQLCommand(SQLCommand.Type.DROP_TABLE, tableName, null);
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
        Pattern pattern = Pattern.compile("update ([\\w]+)\\s+set\\s+([\\w]+)\\s*=\\s*('[^']*'|\\d+|null)(?: where (.+))?", Pattern.CASE_INSENSITIVE);
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
            case DROP_TABLE:
                executeDropTable(jedis, command);
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
            case DROP_TABLE:
                executeDropTable(jedis, command);
                return 0;
            default:
                throw new SQLException("Unsupported command type for update: " + command.type);
        }
    }

    protected void executeCreateTable(Jedis jedis, SQLCommand command) throws SQLException {
        String schemaKey = "schema:" + command.tableName;
        String columnsKey = command.tableName + ":columns";
        String counterKey = command.tableName + ":counter";
        
        // Parse column definitions
        String[] columnDefs = command.columns.split(",");
        Map<String, String> columnTypes = new HashMap<>();
        String primaryKeyColumn = null;
        boolean hasAutoIncrement = false;
        
        for (String columnDef : columnDefs) {
            String[] parts = columnDef.trim().split("\\s+");
            String columnName = parts[0].trim();
            String columnType = parts[1].trim().toUpperCase();
            
            columnTypes.put(columnName, columnType);
            
            // Check for PRIMARY KEY and AUTO_INCREMENT
            if (columnDef.toUpperCase().contains("PRIMARY KEY")) {
                primaryKeyColumn = columnName;
                if (columnDef.toUpperCase().contains("AUTO_INCREMENT")) {
                    hasAutoIncrement = true;
                }
            }
        }
        
        // Store schema information
        Transaction tx = jedis.multi();
        tx.set(schemaKey, command.columns);
        for (Map.Entry<String, String> entry : columnTypes.entrySet()) {
            tx.hset(columnsKey, entry.getKey(), entry.getValue());
        }
        if (hasAutoIncrement) {
            tx.set(counterKey, "0");
        }
        tx.exec();
    }

    protected void executeDropTable(Jedis jedis, SQLCommand command) throws SQLException {
        String schemaKey = "schema:" + command.tableName;
        String columnsKey = command.tableName + ":columns";
        String counterKey = command.tableName + ":counter";
        String keysKey = command.tableName + ":keys";
        
        // Get all record keys before starting transaction
        Set<String> keys = jedis.smembers(keysKey);
        
        // Delete all records and metadata in a single transaction
        Transaction tx = jedis.multi();
        for (String key : keys) {
            tx.del(key);
        }
        tx.del(schemaKey);
        tx.del(columnsKey);
        tx.del(counterKey);
        tx.del(keysKey);
        tx.exec();
    }

    protected int executeInsert(Jedis jedis, SQLCommand command) throws SQLException {
        String[] columnNames = command.columns.split(",");
        String[] values = command.values.split(",");
        
        if (columnNames.length != values.length) {
            throw new SQLException("Column count doesn't match values count");
        }

        // Get table schema
        String columnsKey = command.tableName + ":columns";
        Map<String, String> columnTypes = jedis.hgetAll(columnsKey);
        if (columnTypes.isEmpty()) {
            throw new SQLException("Table " + command.tableName + " does not exist");
        }

        // Create record hash
        Map<String, String> hash = new HashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            String columnName = columnNames[i].trim();
            String value = values[i].trim();
            
            // Handle NULL values
            if (value.equalsIgnoreCase("null")) {
                hash.put(columnName, "__NULL__");
                continue;
            }
            
            // Remove quotes from string values
            if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            
            hash.put(columnName, value);
        }

        // Generate record ID
        String counterKey = command.tableName + ":counter";
        String recordId = String.valueOf(jedis.incr(counterKey));
        String recordKey = command.tableName + ":" + recordId;
        
        // Store record
        Transaction tx = jedis.multi();
        tx.hmset(recordKey, hash);
        tx.sadd(command.tableName + ":keys", recordKey);
        tx.exec();
        
        return 1;
    }

    protected List<Map<String, String>> executeSelect(Jedis jedis, SQLCommand command) throws SQLException {
        Set<String> keys = jedis.smembers(command.tableName + ":keys");
        List<Map<String, String>> results = new ArrayList<>();
        
        // Handle COUNT(*) functionality
        if (command.columns.trim().toLowerCase().startsWith("count(*)")) {
            int count = 0;
            for (String key : keys) {
                Map<String, String> record = jedis.hgetAll(key);
                if (matchesWhereClause(record, command.whereClause)) {
                    count++;
                }
            }
            Map<String, String> countResult = new HashMap<>();
            countResult.put("count", String.valueOf(count));
            results.add(countResult);
            return results;
        }
        
        // Get column names to select
        Set<String> selectedColumns = new HashSet<>();
        if (command.columns.equals("*")) {
            String columnsKey = command.tableName + ":columns";
            selectedColumns.addAll(jedis.hkeys(columnsKey));
        } else {
            for (String column : command.columns.split(",")) {
                selectedColumns.add(column.trim());
            }
        }
        
        // Sort keys to ensure consistent ordering
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        
        for (String key : sortedKeys) {
            Map<String, String> record = jedis.hgetAll(key);
            if (matchesWhereClause(record, command.whereClause)) {
                // Filter columns and handle NULL values
                Map<String, String> filteredRecord = new HashMap<>();
                for (String column : selectedColumns) {
                    String value = record.get(column);
                    filteredRecord.put(column, "__NULL__".equals(value) ? null : value);
                }
                results.add(filteredRecord);
            }
        }
        
        return results;
    }

    protected int executeDelete(Jedis jedis, SQLCommand command) throws SQLException {
        Set<String> keys = jedis.smembers(command.tableName + ":keys");
        int deletedCount = 0;
        
        for (String key : keys) {
            Map<String, String> record = jedis.hgetAll(key);
            if (matchesWhereClause(record, command.whereClause)) {
                Transaction tx = jedis.multi();
                tx.del(key);
                tx.srem(command.tableName + ":keys", key);
                tx.exec();
                deletedCount++;
            }
        }
        
        return deletedCount;
    }

    protected int executeUpdate(Jedis jedis, SQLCommand command) throws SQLException {
        Set<String> keys = jedis.smembers(command.tableName + ":keys");
        int updatedCount = 0;
        
        for (String key : keys) {
            Map<String, String> record = jedis.hgetAll(key);
            if (matchesWhereClause(record, command.whereClause)) {
                // Handle NULL values
                String value = command.values;
                if (value.equalsIgnoreCase("null")) {
                    value = "__NULL__";
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                record.put(command.columns, value);
                jedis.hmset(key, record);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    protected boolean matchesWhereClause(Map<String, String> record, String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            return true;
        }
        
        // Basic WHERE clause parsing - only handles simple equality
        String[] parts = whereClause.split("=");
        if (parts.length != 2) {
            return false;
        }
        
        String column = parts[0].trim();
        String value = parts[1].trim();
        
        // Remove quotes from string values
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        
        String recordValue = record.get(column);
        if (recordValue == null || "__NULL__".equals(recordValue)) {
            return value.equalsIgnoreCase("null");
        }
        
        return recordValue.equals(value);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        checkClosed();
        SQLCommand command = parseSQLCommand(sql);
        
        try (Jedis jedis = connection.getJedis()) {
            switch (command.type) {
                case SELECT:
                    List<Map<String, String>> results = executeRedisCommand(jedis, command);
                    currentResultSet = new RedisResultSet(this, results);
                    currentUpdateCount = -1;
                    return true;
                case DELETE:
                case UPDATE:
                case INSERT:
                    currentUpdateCount = executeRedisUpdate(jedis, command);
                    currentResultSet = null;
                    return false;
                case CREATE_TABLE:
                case DROP_TABLE:
                    executeRedisUpdate(jedis, command);
                    currentResultSet = null;
                    currentUpdateCount = 0;
                    return false;
                default:
                    throw new SQLException("Unsupported command type: " + command.type);
            }
        }
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return currentResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return currentUpdateCount;
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
            DROP_TABLE,
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

    protected void checkClosed() throws SQLException {
        if (isClosed) {
            throw new SQLException("Statement is closed");
        }
    }

    @Override
    public void close() throws SQLException {
        if (!isClosed) {
            isClosed = true;
            currentResultSet = null;
            currentUpdateCount = -1;
        }
    }
} 