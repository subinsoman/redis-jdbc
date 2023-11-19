# README

It is a JDBC Driver for Redis which is based on Jedis.

URL: 

* GitHub地址：https://github.com/eacdy/redis-jdbc


## Features

* Supports both single-node Redis sentinetal,and Redis Cluster.
* Supports all the Redis Command that Jedis supports.
* Support all JDBC-based ORM frameworks(aka. Mybatis or Hibernate).

## How to use it?

Add the driver to your project:

```xml
 <dependency>
    <groupId>org.apache.redis</groupId>
    <artifactId>redis-jdbc</artifactId>
    <version>0.0.1</version>
</dependency>
```

### For Redis

Just use like below:

```java
Class.forName("org.apache.redis.jdbc.RedisDriver");

Connection connection = DriverManager.getConnection(
  "jdbc:redis://localhost:6379/0",
  properties
);
Statement statement = connection.createStatement();

connection.setSchema("11");
ResultSet rs = statement.executeQuery("get a");
while (rs.next()) {
  String string = rs.getString(0);
  System.out.println(string);
}
```

The properties can be like below:

| key      | defaultValue | description          |
| -------- | ------------ | -------------------- |
| user     | null         | the user of Redis    |
| password | null         | the password of user |
| ssl      | false        | whether to use ssl   |
| timeout  | 1000         | Jedis timeout        |

### For Redis Cluster

Just use like below:

```java
Class.forName("com.itmuch.redis.jdbc.cluster.RedisClusterDriver");

Connection connection = DriverManager.getConnection(
  "jdbc:redis-cluster:///localhost:6379;localhost:6380;localhost:6381",
  properties
);
Statement statement = connection.createStatement();

connection.setSchema("11");
ResultSet rs = statement.executeQuery("get a");
while (rs.next()) {
  String string = rs.getString(0);
  System.out.println(string);
}
```


The properties can be like below:

| key         | defaultValue | description          |
| ----------- | ------------ | -------------------- |
| user        | null         | the user of Redis    |
| password    | null         | the password of user |
| ssl         | false        | whether to use ssl   |
| timeout     | 1000         | Jedis timeout        |
| maxAttempts | 5            | Jedis maxAttempts    |


### For Redis Sentinel

Not Support yet.

## Thanks

