# RabbitMQ Data Replication

A Java implementation of a distributed data replication system using RabbitMQ fanout exchanges. Demonstrates write broadcasting, high-availability reads, failure simulation, and majority-vote consistency.

---

## Architecture

```
ClientWriter ──► write_exchange (fanout) ──► replica_queue_1 ──► Replica 1 ──► replica_1/data.txt
                                         ──► replica_queue_2 ──► Replica 2 ──► replica_2/data.txt
                                         ──► replica_queue_3 ──► Replica 3 ──► replica_3/data.txt

ClientReader ──► read_exchange (fanout)  ──► replica_queue_1 ──► Replica 1 ──┐
                                         ──► replica_queue_2 ──► Replica 2 ──┼──► replyQueue ──► ClientReader
                                         ──► replica_queue_3 ──► Replica 3 ──┘        (first response wins)
```

Each replica owns a single queue bound to **both** exchanges. Queues are `auto-delete`, so they disappear when a replica stops — intentionally simulating message loss during a node failure.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| Maven | 3.6+ |
| RabbitMQ | Any recent version, running on `localhost:5672` |

---

## Build

```bash
mvn clean package -q
```

Produces a fat JAR at `target/rabbitmq-replication-1.0-SNAPSHOT.jar` (all dependencies bundled via `maven-shade-plugin`).

---

## Running

### 1. Start RabbitMQ

```bash
rabbitmq-server
```

### 2. Start the three replicas

Each replica should run in its own terminal:

```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica 1
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica 2
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica 3
```

Or use the provided script (Windows):

```bash
start_replicas.bat
```

---

## Usage

### Writing data

Broadcasts a line to all replicas simultaneously:

```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientWriter <line_number> <text...>

# Example
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientWriter 1 Hello world
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientWriter 2 Another line
```

Each replica appends the line to its local `replica_N/data.txt` file.

### Reading (last line, first response)

```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientReader
```

Sends a `READ_LAST` request to all replicas and returns whichever replica responds first. If one replica is down, the others still answer — demonstrating high availability.

### Reading (majority vote)

```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientReaderV2
```

Sends a `READ_ALL` request, collects every line from every available replica, and displays only lines present in **at least 2 out of 3 replicas**. This resolves inconsistencies introduced by node failures.

---

## Failure Simulation

`demo_q6.bat` walks through the following scenario to demonstrate data inconsistency and how `ClientReaderV2` resolves it:

1. Write lines 1 and 2 — all 3 replicas in sync
2. **Stop Replica 2**
3. Write lines 3 and 4 — Replica 2 misses them (its queue was auto-deleted)
4. **Restart Replica 2** — it comes back with only lines 1 and 2
5. Run `ClientReaderV2` — majority vote shows lines 1–4 (present in Replicas 1 & 3), excluding any data unique to the stale replica

```bash
demo_q6.bat
```

After the simulation, inspect the replica files directly to observe the divergence:

```
replica_1/data.txt  ← lines 1, 2, 3, 4
replica_2/data.txt  ← lines 1, 2         (stale)
replica_3/data.txt  ← lines 1, 2, 3, 4
```

---

## Message Protocol

| Message | Direction | Meaning |
|---------|-----------|---------|
| `WRITE:<line>` | Client → Replica | Append a line to the local file |
| `READ_LAST` | Client → Replica | Reply with the last non-empty line |
| `READ_ALL` | Client → Replica | Reply with all lines, then `END` |
| `LINE:<content>` | Replica → Client | One line in a `READ_ALL` response |
| `END` | Replica → Client | Signals end of a `READ_ALL` response |

---

## Project Structure

```
├── src/main/java/com/tp/replication/
│   ├── RabbitMQConfig.java     # Shared constants (host, exchanges, timeouts)
│   ├── Replica.java            # Replica process — handles WRITE, READ_LAST, READ_ALL
│   ├── ClientWriter.java       # Broadcasts a write to all replicas
│   ├── ClientReader.java       # Reads last line (first-response wins)
│   └── ClientReaderV2.java     # Reads all lines with majority-vote filtering
├── replica_1/data.txt
├── replica_2/data.txt
├── replica_3/data.txt
├── start_replicas.bat          # Launches all 3 replicas (Windows)
├── demo_q6.bat                 # Interactive failure simulation (Windows)
└── pom.xml
```
