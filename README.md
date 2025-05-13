# Redis JDBC Driver

[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](https://github.com/subinsoman/redis-jdbc/releases/tag/v1.0.2)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/java-%3E%3D8-orange.svg)](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)

A JDBC driver implementation that allows using SQL-like commands with Redis as the backend storage. This driver enables applications to interact with Redis using familiar SQL syntax while leveraging Redis's high-performance data structures.

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [SQL Support](#sql-support)
- [Development](#development)
- [Testing](#testing)
- [Release Notes](#release-notes)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core Features
- Full JDBC 4.0 compliance
- Support for multiple Redis deployment modes:
  - Standalone mode
  - Cluster mode
  - Sentinel mode
- Connection pooling
- Transaction support
- Prepared statements with parameter binding
- Auto-incrementing primary keys
- Table schema management

### SQL Operations Support
- CREATE TABLE with schema management
- INSERT with auto-incrementing primary keys
- SELECT with WHERE clause filtering
- UPDATE with WHERE clause
- DELETE with WHERE clause
- DROP TABLE
- COUNT(*) aggregation support

## Requirements

- Java 8 or higher
- Redis 6.x or higher
- Maven 3.6.x or higher
- Jedis 5.1.0

## Installation

1. Clone the repository:
```bash
git clone https://github.com/subinsoman/redis-jdbc.git
```

2. Build the project:
```bash
mvn clean package
```

3. Add the dependency to your project:
```xml
<dependency>
    <groupId>org.apache.redis</groupId>
    <artifactId>redis-jdbc</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Redis Configuration

### Standalone Mode

1. **Redis Server Configuration**
```bash
# redis.conf
port 6379
bind 127.0.0.1
requirepass your_password  # Optional
databases 16
```

2. **JDBC Connection**
```java
String url = "jdbc:redis://localhost:6379/0";
Properties props = new Properties();
props.setProperty("password", "your_password");  // Optional
props.setProperty("timeout", "2000");           // Connection timeout in milliseconds
props.setProperty("database", "0");             // Redis database number (0-15)

Connection conn = DriverManager.getConnection(url, props);
```

### Cluster Mode

1. **Redis Cluster Configuration**
```bash
# For each cluster node (e.g., 7000.conf, 7001.conf, etc.)
port 7000                          # Different for each node
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
protected-mode no
bind 0.0.0.0                       # Allow external connections
```

2. **Start Cluster Nodes**
```bash
# Start each node
redis-server 7000.conf &
redis-server 7001.conf &
redis-server 7002.conf &

# Create cluster (Redis 5.0+)
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
    --cluster-replicas 0
```

3. **JDBC Connection**
```java
// Multiple nodes in connection URL
String url = "jdbc:redis:cluster://127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002";
Properties props = new Properties();
props.setProperty("password", "your_password");     // Optional
props.setProperty("maxRetries", "3");              // Max retry attempts
props.setProperty("connectTimeout", "2000");        // Connection timeout
props.setProperty("soTimeout", "2000");            // Socket timeout

Connection conn = DriverManager.getConnection(url, props);
```

### Sentinel Mode

1. **Redis Sentinel Configuration**
```bash
# sentinel.conf
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
sentinel auth-pass mymaster your_master_password    # If master has password
```

2. **Start Master, Slaves, and Sentinels**
```bash
# Start master
redis-server master.conf &

# Start slaves
redis-server slave1.conf --slaveof 127.0.0.1 6379 &
redis-server slave2.conf --slaveof 127.0.0.1 6379 &

# Start sentinels
redis-server sentinel1.conf --sentinel &
redis-server sentinel2.conf --sentinel &
redis-server sentinel3.conf --sentinel &
```

3. **JDBC Connection**
```java
// Multiple sentinel addresses in URL
String url = "jdbc:redis:sentinel://sentinel1:26379,sentinel2:26379,sentinel3:26379/0";
Properties props = new Properties();
props.setProperty("masterName", "mymaster");        // Required: master name from sentinel.conf
props.setProperty("password", "your_password");     // Optional: Redis password
props.setProperty("sentinelPassword", "sentinel_auth_pass");  // Optional: Sentinel password
props.setProperty("database", "0");                 // Redis database number
props.setProperty("maxRetries", "3");              // Max retry attempts
props.setProperty("timeout", "2000");              // Connection timeout

Connection conn = DriverManager.getConnection(url, props);
```

### Connection Properties Reference

| Property | Mode | Description | Default |
|----------|------|-------------|---------|
| password | All | Redis server password | null |
| database | Standalone, Sentinel | Redis database number (0-15) | 0 |
| timeout | All | Connection timeout in milliseconds | 2000 |
| maxRetries | Cluster, Sentinel | Maximum retry attempts | 3 |
| masterName | Sentinel | Name of master node in sentinel config | Required |
| sentinelPassword | Sentinel | Password for sentinel nodes | null |
| connectTimeout | All | Socket connection timeout | 2000 |
| soTimeout | All | Socket read timeout | 2000 |
| clientName | All | Client name for connection | null |
| ssl | All | Enable SSL connection | false |
| sslSocketFactory | All | Custom SSL socket factory | null |

## Usage

### Connection URLs

The driver supports three types of Redis deployments:

1. **Standalone Redis**
```java
// Basic connection
String url = "jdbc:redis://localhost:6379/0";
Properties info = new Properties();
info.setProperty("password", "optional_password");
Connection conn = DriverManager.getConnection(url, info);
```

2. **Redis Cluster**
```java
// Cluster connection
String url = "jdbc:redis:cluster://node1:6379,node2:6379,node3:6379";
Properties info = new Properties();
info.setProperty("password", "optional_password");
Connection conn = DriverManager.getConnection(url, info);
```

3. **Redis Sentinel**
```java
// Sentinel connection
String url = "jdbc:redis:sentinel://sentinel1:26379,sentinel2:26379,sentinel3:26379/0";
Properties info = new Properties();
info.setProperty("password", "optional_password");
info.setProperty("masterName", "mymaster"); // Required for Sentinel
Connection conn = DriverManager.getConnection(url, info);
```

### SQL Examples

1. **Create Table**
```java
Statement stmt = conn.createStatement();
stmt.execute("""
    CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(255),
        email VARCHAR(255),
        created_at TIMESTAMP
    )
""");
```

2. **Insert Data**
```java
// Using Statement
stmt.execute("INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')");

// Using PreparedStatement
PreparedStatement pstmt = conn.prepareStatement(
    "INSERT INTO users (name, email) VALUES (?, ?)"
);
pstmt.setString(1, "Jane Doe");
pstmt.setString(2, "jane@example.com");
pstmt.executeUpdate();
```

3. **Select Data**
```java
// Simple SELECT
ResultSet rs = stmt.executeQuery("SELECT * FROM users");

// SELECT with WHERE clause
PreparedStatement pstmt = conn.prepareStatement(
    "SELECT name, email FROM users WHERE name = ?"
);
pstmt.setString(1, "John Doe");
ResultSet rs = pstmt.executeQuery();

// Using COUNT(*)
ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
```

4. **Update Data**
```java
PreparedStatement pstmt = conn.prepareStatement(
    "UPDATE users SET email = ? WHERE name = ?"
);
pstmt.setString(1, "new.email@example.com");
pstmt.setString(2, "John Doe");
pstmt.executeUpdate();
```

5. **Delete Data**
```java
PreparedStatement pstmt = conn.prepareStatement(
    "DELETE FROM users WHERE name = ?"
);
pstmt.setString(1, "John Doe");
pstmt.executeUpdate();
```

### Transaction Support

```java
Connection conn = DriverManager.getConnection(url, info);
conn.setAutoCommit(false);
try {
    Statement stmt = conn.createStatement();
    stmt.executeUpdate("INSERT INTO users (name) VALUES ('User 1')");
    stmt.executeUpdate("INSERT INTO users (name) VALUES ('User 2')");
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true);
}
```

## Development

### Project Structure

```
redis-jdbc/
├── src/
│   ├── main/java/org/apache/redis/jdbc/
│   │   ├── RedisDriver.java
│   │   ├── RedisConnection.java
│   │   ├── RedisStatement.java
│   │   ├── RedisPreparedStatement.java
│   │   └── RedisResultSet.java
│   └── test/java/org/apache/redis/jdbc/
│       ├── RedisConnectionTest.java
│       ├── RedisStatementTest.java
│       └── RedisPreparedStatementTest.java
├── pom.xml
├── README.md
└── RELEASE_NOTES.md
```

### Building from Source

1. Install dependencies:
```bash
mvn clean install
```

2. Run tests:
```bash
mvn test
```

3. Generate documentation:
```bash
mvn javadoc:javadoc
```

## Testing

The driver includes comprehensive unit tests covering:

- Connection management
- Statement execution
- PreparedStatement parameter binding
- ResultSet navigation
- Transaction handling
- Error conditions and edge cases

Run tests using:
```bash
mvn test
```

## Release Notes

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for detailed information about each release.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style Guidelines

- Follow Java coding conventions
- Write comprehensive unit tests for new features
- Update documentation for significant changes
- Add appropriate error handling
- Maintain backward compatibility

## Support

For support:
- Open an issue in the GitHub repository
- Contact the maintainers
- Check the [documentation](docs/)

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.

## Acknowledgments

- Redis development team
- Apache Software Foundation
- JDBC specification team
- Contributors and users of the project 