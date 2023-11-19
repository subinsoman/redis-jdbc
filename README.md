{
  "version": "1.0",
  "defaultSchema": "foodmart",
  "schemas": [
    {
      "type": "custom",
      "name": "foodmart",
      "factory": "org.apache.calcite.adapter.redis.RedisSchemaFactory",
      "operand": {
        "host": "localhost",
        "port": 6379,
        "database": 0,
        "password": ""
      },
      "tables": [
        {
          "name": "json_01",
          "factory": "org.apache.calcite.adapter.redis.RedisTableFactory",
          "operand": {
            "dataFormat": "json",
            "fields": [
              {
                "name": "DEPTNO",
                "type": "varchar",
                "mapping": "DEPTNO"
              },
              {
                "name": "NAME",
                "type": "varchar",
                "mapping": "NAME"
              }
            ]
          }
        },
        {
          "name": "raw_01",
          "factory": "org.apache.calcite.adapter.redis.RedisTableFactory",
          "operand": {
            "dataFormat": "raw",
            "fields": [
              {
                "name": "id",
                "type": "varchar",
                "mapping": "id"
              },
              {
                "name": "city",
                "type": "varchar",
                "mapping": "city"
              },
              {
                "name": "pop",
                "type": "int",
                "mapping": "pop"
              }
            ]
          }
        },
        {
          "name": "csv_01",
          "factory": "org.apache.calcite.adapter.redis.RedisTableFactory",
          "operand": {
            "dataFormat": "csv",
            "keyDelimiter": ":",
            "fields": [
              {
                "name": "EMPNO",
                "type": "varchar",
                "mapping": 0
              },
              {
                "name": "NAME",
                "type": "varchar",
                "mapping": 1
              }
            ]
          }
        }
      ]
    }
  ]
}# README

It is a JDBC Driver for Redis  that supports sql

URL: 

* GitHub：https://github.com/subinsoman/redis-jdbc


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
  "jdbc:redis:model=redis/src/main/resources/redis-model.json",
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
  "jdbc:redis:model=redis/src/main/resources/redis-model.json",
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

PLEASE CONTRIBUTE TO SUPPORT