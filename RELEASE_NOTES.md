# Release Notes

## Version 1.0.0 (2024-03-21)

### Features
- Initial release of Redis JDBC Driver
- Support for Redis deployment modes:
  - Standalone mode (jdbc:redis://host:port/database)
  - Cluster mode (jdbc:redis:cluster://node1:port1,node2:port2,...)
  - Sentinel mode (jdbc:redis:sentinel://sentinel1:port1,sentinel2:port2,.../database)
- Jedis client integration (version 5.1.0)
- Full JDBC 4.0 compliance

### SQL Support
- Basic SQL operations:
  - CREATE TABLE with schema management
  - INSERT with auto-incrementing primary keys
  - SELECT with WHERE clause filtering
  - UPDATE with WHERE clause
  - DELETE with WHERE clause
  - DROP TABLE
- Prepared statements with parameter binding
- Transaction support
- Connection pooling

### Data Features
- NULL value handling using "__NULL__" marker
- Proper transaction handling for operations
- Consistent row ordering in result sets
- COUNT(*) aggregation support
- Table schema management
- Auto-incrementing primary keys

### Testing
- Comprehensive test suite for:
  - RedisConnection
  - RedisStatement
  - RedisPreparedStatement
- Test coverage for:
  - Basic CRUD operations
  - NULL value handling
  - Transaction management
  - Connection pooling
  - Statement resource cleanup
  - Parameter validation

### Technical Details
- Package structure reorganized from com.example.jdbc to org.apache.redis.jdbc
- Maven configuration with JUnit Jupiter and Mockito for testing
- Proper connection type handling with java.sql.Connection.TRANSACTION_NONE
- Improved resource management and cleanup
- Enhanced error handling and validation 