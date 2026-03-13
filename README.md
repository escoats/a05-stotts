# Four Rings

Team: Elizabeth Coats, Manasi Chaudhary, Emma Coye

## Overview

This project implements a concurrent system that is made up of four rings of nodes. Each ring processes a token derived from user input; this system was implemented in both Elixir and Java.

A central coordinator reads integer inputs from the user and calls one of the four rings based on input value:

- NEG: for inputs less than 0
- ZERO: for inputs equal to 0
- POS_EVEN: for inputs that are positive, even numbers
- POS_ODD: for inputs that are positive, odd numbers

Each ring contains N nodes arranged in a directed cycle, so when an input is given the token is made and circulated through the ring for H hops. Each node will apply the specific transformation to the value, and once the hop count is 0 the final result is given back to the coordinator and then printed.
Rings process one token at a time.

## Implementation Structure

### Elixir

The Elixir implementation uses spawned processes to represent nodes in each ring.

#### File Structure

- `elixir/main.ex`
  - `start/2` and `input_loop/5` separate coordination from worker behavior, making routing policy and concurrency control explicit and easier to verify.
  - `build_ring/3` spawns each node process, wires it to the next node, and creates a gatekeeper.
  - `gatekeeper/2` maintains the one-token-at-a-time rule per ring using nested `receive` blocks. The first block passes an input into the ring, and the second acts as a blocker so that the next token can only enter the ring once the previous token has completed.
  - `node_loop/2` executes the given computation for its ring type, then either forwards the token to the next node, or reports completion to the gatekeeper process if it is the last hop. Finally, it waits to receive the next token.
- `elixir/beamMon.ex`
  - BEAM runtime monitor.

### Java: File Roles and Design Rationale

- `java/Main.java`
  - Central coordinator for input parsing, routing, and lifecycle control. This separation prevents coupling between input concerns and ring-internal worker logic.
  - Uses a completion queue and dedicated printer thread to isolate I/O latency from token-processing throughput.
- `java/Ring.java`
  - Ring-level concurrency manager that owns pending-queue state, token-in-flight state, node wiring, and shutdown policy.
  - Centralizes invariants that exceed node scope, including FIFO submission order, single active token admission, and safe injection semantics.
- `java/RingNode.java`
  - Isolated per-node worker thread that preserves local, deterministic hop-processing behavior.
  - Uses blocking inbox semantics to avoid busy-waiting and unnecessary scheduler contention.
- `java/RingType.java`
  - Centralizes transformation strategy by ring type so worker threads do not embed ring-identity branching logic.
- `java/Token.java`
  - Shared message payload object. Identity fields are immutable, while processing fields remain mutable to avoid per-hop object allocation.

## Concurrency Design Rationale

### Elixir: Processes and Mailboxes

Elixir processes are lightweight and map naturally to an actor-style node model. Mailboxes maintain a FIFO queue without having to explicitly define or coordinate ordering. Nodes are kept alive through tail recursion.

### Elixir: Gatekeeper-Based Admission Control

- The gatekeeper establishes an explicit admission boundary and serializes ring entry.
- A nested `receive` acts as a completion barrier, ensuring token `k+1` is not admitted until token `k` completes.
- This approach enforces one-token-per-ring behavior without explicit locking.

### Java: Threads and BlockingQueue

Nodes are represented by Java threads. Waiting and FIFO ordering is achieved using `BlockingQueue`.

### Java: Separation of Ring Policy from Node Behavior

- Node threads are restricted to hop-level behavior.
- Ring-wide policies (queueing, injection, and shutdown sequencing) are centralized in `Ring.java` to avoid race-prone policy duplication.
- `AtomicBoolean tokenInFlight` tracks the single-active-token invariant at ring scope.
- `synchronized tryInject()` prevents concurrent injection paths from violating that invariant during submit/complete races.

### Java: Dedicated Completion Printer Thread

- Console output is comparatively slow and potentially blocking.
- If worker threads print directly, overall throughput and responsiveness become coupled to I/O latency.
- A completion queue plus dedicated printer thread keeps the critical token path focused on computation and message transfer.

### Shutdown Strategy

- Elixir: a `:done` message is sent to each gatekeeper, preventing additonal tokens from entering the queue. The `:done` message is also sent to each node, which do not recurse again once given the shutdown message. Because mailbox processing is ordered, previously-queued `:input` messages are processed before `:done` is handled.
- Java: the ring manager waits until the pending queue is empty and no token remains in flight, then injects a poison token to terminate node threads in order.
