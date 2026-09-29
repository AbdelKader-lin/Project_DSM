# Distributed Shared Memory with RabbitMQ

A distributed systems project implementing a simple Distributed Shared Memory (DSM) abstraction in Java. Multiple independent processes cooperate over RabbitMQ while exposing a shared address space to the user.

## Overview

Each DSM node runs as a separate Java process with its own local state. Nodes communicate through RabbitMQ to coordinate read and write operations on global addresses.

The command-line interface supports synchronous and asynchronous memory operations, making it possible to experiment with how shared-memory semantics can be implemented on top of message passing.

## Features

* multiple distributed DSM nodes
* global memory addresses shared across processes
* RabbitMQ-based inter-node communication
* synchronous reads and writes
* asynchronous reads and writes
* local memory snapshots for observing node state
* Maven-based Java project structure

## Project structure

The Java implementation is under `src/main/java/dsm` and is separated into model, service, utility and exception packages. `Main.java` starts a DSM node and exposes the command-line interface.

The repository also contains the project specification and the final project report.

## Build and run

A RabbitMQ server must be running locally.

Build the project:

```bash
mvn clean package
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
```

Start one process for each DSM node:

```bash
java -cp "target/classes:$(cat cp.txt)" dsm.Main <nodeId> <totalNodes> <globalAddresses>
```

Node IDs start at `0`. Once all nodes are running, the CLI accepts commands such as:

```text
r <address>
w <address> <value>
ra <address>
wa <address> <value>
print
```

`r` and `w` perform synchronous operations. `ra` and `wa` are their asynchronous counterparts. `print` displays the node's local memory snapshot.

## Concepts explored

Distributed shared memory, message passing, distributed coordination, synchronous and asynchronous communication, RabbitMQ, Java and Maven.

## Context

Developed as a distributed systems project during the MOSIG master's program at Université Grenoble Alpes.