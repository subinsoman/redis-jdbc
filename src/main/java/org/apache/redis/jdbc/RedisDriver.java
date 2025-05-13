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
import java.util.Properties;
import java.util.logging.Logger;

public class RedisDriver implements Driver {
    private static final String URL_PREFIX = "jdbc:redis://";
    private static final String CLUSTER_PREFIX = "jdbc:redis:cluster://";
    private static final String SENTINEL_PREFIX = "jdbc:redis:sentinel://";
    
    static {
        try {
            DriverManager.registerDriver(new RedisDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Can't register Redis Driver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        return new RedisConnection(url, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && (
            url.startsWith(URL_PREFIX) ||
            url.startsWith(CLUSTER_PREFIX) ||
            url.startsWith(SENTINEL_PREFIX)
        );
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        DriverPropertyInfo[] driverProps = new DriverPropertyInfo[3];
        
        driverProps[0] = new DriverPropertyInfo("password", info.getProperty("password"));
        driverProps[0].description = "Redis password for authentication";
        driverProps[0].required = false;
        
        driverProps[1] = new DriverPropertyInfo("masterName", info.getProperty("masterName"));
        driverProps[1].description = "Master name for Redis Sentinel";
        driverProps[1].required = false;
        
        driverProps[2] = new DriverPropertyInfo("maxRetries", info.getProperty("maxRetries", "3"));
        driverProps[2].description = "Maximum number of retries for cluster/sentinel operations";
        driverProps[2].required = false;
        
        return driverProps;
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
} 