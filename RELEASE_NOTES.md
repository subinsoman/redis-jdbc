# Release Notes

## Version 1.0.1 (2024-03-21)

### Improvements
- Enhanced Redis deployment configuration documentation:
  - Added detailed Standalone mode setup guide
  - Added comprehensive Cluster mode configuration
  - Added complete Sentinel mode setup instructions
- Improved connection properties documentation with detailed reference table
- Added security best practices and recommendations
- Enhanced SQL examples with transaction handling
- Updated README with status badges and version information

## Version 1.0.2 (2024-03-22)

### Bug Fixes
- Fixed authentication test to properly handle Redis authentication errors:
  - Updated test to expect correct exception type (JedisDataException) for authentication failures
  - Improved test cases for both authenticated and non-authenticated connections
  - Enhanced error message clarity for authentication-related failures
- Fixed cluster mode connection issues:
  - Corrected exception handling for cluster node authentication
  - Improved cluster node connection retry logic
  - Enhanced error reporting for cluster configuration issues
- Fixed sentinel mode connection handling:
  - Corrected sentinel authentication mechanism
  - Fixed master discovery process in sentinel mode
  - Improved failover handling and connection recovery

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

### Documentation
- Added comprehensive deployment configuration guides:
  - Standalone Redis setup and configuration
  - Redis Cluster setup with multiple nodes
  - Redis Sentinel setup with master-slave configuration
- Enhanced connection properties documentation
- Added detailed SQL examples and usage patterns
- Improved project structure documentation
- Added status badges and version information
- Updated installation and build instructions
- Added transaction handling examples
- Included security best practices 