# Redis JDBC Driver

A JDBC driver implementation that allows using SQL-like commands with Redis as the backend storage. This driver enables applications to interact with Redis using familiar SQL syntax while leveraging Redis's high-performance data structures.

## Table of Contents
- [Architecture](#architecture)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [SQL Support](#sql-support)
- [Development](#development)
- [Testing](#testing)
- [License](#license)

## Architecture

### Core Components

1. **RedisDriver (`org.apache.redis.jdbc.RedisDriver`)**
   - Implements `java.sql.Driver`
   - Handles JDBC URL parsing and connection creation
   - URL format: `jdbc:redis://[host]:[port]/[database]`

2. **RedisConnection (`org.apache.redis.jdbc.RedisConnection`)**
   - Implements `java.sql.Connection`
   - Manages Redis connection lifecycle
   - Handles transaction support
   - Creates Statement and PreparedStatement instances

3. **RedisStatement (`org.apache.redis.jdbc.RedisStatement`)**
   - Implements `java.sql.Statement`
   - Executes SQL queries
   - Translates SQL operations to Redis commands

4. **RedisPreparedStatement (`org.apache.redis.jdbc.RedisPreparedStatement`)**
   - Implements `java.sql.PreparedStatement`
   - Handles parameterized queries
   - Provides SQL injection protection

5. **RedisResultSet (`org.apache.redis.jdbc.RedisResultSet`)**
   - Implements `java.sql.ResultSet`
   - Represents query results
   - Provides cursor-based data access

### Data Storage Model

The driver uses Redis data structures to store and manage relational data:

- **Tables**: Represented as Redis key namespaces
- **Records**: Stored as Redis hashes
- **Indices**: Implemented using Redis sets
- **Primary Keys**: Managed using Redis incremental counters

## Features

- Full JDBC 4.0 compliance
- Support for basic SQL operations (CREATE, INSERT, SELECT, UPDATE, DELETE)
- Transaction support
- Prepared statements with parameter binding
- Connection pooling
- Auto-incrementing primary keys
- Table schema management

## Requirements

- Java 8 or higher
- Redis 6.x or higher
- Maven 3.6.x or higher

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/redis-jdbc.git
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
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Basic Connection

```java
String url = "jdbc:redis://localhost:6379/0";
Connection conn = DriverManager.getConnection(url);
```

### Execute SQL Queries

```java
// Create table
Statement stmt = conn.createStatement();
stmt.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), email VARCHAR(255))");

// Insert data
stmt.execute("INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')");

// Select data
ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE name = 'John Doe'");
while (rs.next()) {
    System.out.println("ID: " + rs.getInt("id"));
    System.out.println("Name: " + rs.getString("name"));
    System.out.println("Email: " + rs.getString("email"));
}
```

### Using Prepared Statements

```java
PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (name, email) VALUES (?, ?)");
pstmt.setString(1, "Jane Doe");
pstmt.setString(2, "jane@example.com");
pstmt.executeUpdate();
```

## SQL Support

### Supported Operations

1. **CREATE TABLE**
   ```sql
   CREATE TABLE table_name (
       column1 datatype PRIMARY KEY AUTO_INCREMENT,
       column2 datatype,
       column3 datatype
   )
   ```

2. **INSERT**
   ```sql
   INSERT INTO table_name (column1, column2) VALUES (value1, value2)
   ```

3. **SELECT**
   ```sql
   SELECT * FROM table_name WHERE column1 = value1
   SELECT column1, column2 FROM table_name
   ```

4. **UPDATE**
   ```sql
   UPDATE table_name SET column1 = value1 WHERE column2 = value2
   ```

5. **DELETE**
   ```sql
   DELETE FROM table_name WHERE column1 = value1
   ```

### Supported Data Types

- INTEGER
- BIGINT
- VARCHAR
- TEXT
- TIMESTAMP
- BOOLEAN
- DOUBLE

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
└── README.md
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

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

---

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Support

For support, please open an issue in the GitHub repository or contact the maintainers.

## Acknowledgments

- Redis development team
- Apache Software Foundation
- JDBC specification team 