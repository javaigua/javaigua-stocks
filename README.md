# Stocks System
A take-home-assigment, by Javier Igua.

This Stocks application is a reactive one:
* It is responsive: to ensure a high-quality user experience, this app aims for high throughput and low latency. Its footprint will increase by the number of stocks registered, in which case vertical scaling (up) and even horizontal scaling (out) can be easily considered and integrated. 
* It is resilient: Aspects like delegation, isolation and containtment (TODO: replication) are handled and can be optimized with a relative low complexity added to the codebase.
* It is elastic: Different workloads are supported with the same guaranties by scaling out to a much bigger cluster of nodes. Replication can by addressed by means of distributed data replication (Akka cluster singleton or conflict free distributed data types modules) and/or sharding.
* It is message-driven: Components of this application are decoupled and exchange inmutable messages in asynchronous non-blocking manner. Boundaries between them are kept by these messages, where processing is isolated and performed in first-come first-served semantics.

A set of RESTful (Akka Http) web services are supported to create, retrieve, update and delete stocks. JSON is the format for the messages sent and received by this system.

## Implementations details

The main entry point of this application is the  [StocksServer.java](src/main/java/com/javaigua/stocks/api/StocksServer.java). An Akka actor system is created and binded to an http server that handles requests to create, retrieve, update and delete stocks from an internal stock registry.

Routes for Akka Http processing can be found in [StockRoutes.java](src/main/java/com/javaigua/stocks/api/StockRoutes.java).

An instance of the actor [StockRegistryActor.java](src/main/java/com/javaigua/stocks/actors/StockRegistryActor.java) is a distributed registry for stocks handled in this application. Every stock entity has an actor supervised by this one. An actor reference lookup by the given stock id is performed upon arrival of every message sent to this actor, new ones are created if needed. Messages are then forwarded to that stock actor, except when the whole list of stocks is asked in which case all children data is aggreagated and returned by iterating through all supervised actors created by this one.

Every instance of the  [StockActor.java](src/main/java/com/javaigua/stocks/actors/StockActor.java) controls a single Stock entity. It performs operations on a single Stock instance variable. Messages sent to this actor mutate or query this domain object. Since messages are processed sequentially one-by-one, there is no need to worry about concurrent modifications or other side effects.

Messages shared between actors can be found in [StockRegistryMessages.java](src/main/java/com/javaigua/stocks/actors/StockRegistryMessages.java).

Entities of this application are defined in [Domain.java](src/main/java/com/javaigua/stocks/Domain.java).

## Execution

### To build a jar with dependencies
First build the jar packaje:
```
mvn clean package
```

### Run a cluster with a single node
Now run a cluster of a single node:
```
java -jar target/stocks-javaigua-1.0-with-dependencies.jar
```
An example of http requests with the curl command line utility can be found in [curl_commands.txt](src/test/resources/curl_commands.txt).

### Add another node to the cluster

Add a new node to the cluster: 
```
java -Dapplication.exposed-port=8081 -Dclustering.port=2552 -jar target/stocks-javaigua-1.0-with-dependencies.jar
```
FIXME: This cluster does not share data yet. Try implementing Akka Cluster Singleton or Akka Sharding for the registry.

### To run with maven
```
mvn compile exec:java
```

### Unit testing 
```
mvn compile test
```