# Four Concurrent Rings — Java Implementation

## Overview

This Java implementation builds four independent rings of threads that process integers concurrently. A central Coordinator reads input, routes each integer to one of four rings based on value, and prints completion messages.

## Architecture

### Components

| File | Purpose |
|------|---------|
| `Main.java` | Entry point and Coordinator: reads input, routes to rings, prints completions |
| `Token.java` | Work unit carrying token_id, ring_id, orig_input, current_val, remaining_hops |
| `RingType.java` | Enum for NEG, ZERO, POS_EVEN, POS_ODD with their transformation functions |
| `RingNode.java` | Single node in a ring; runs as a thread, processes tokens and forwards |
| `Ring.java` | Manages one ring: nodes, pending queue, token injection, shutdown |

### Concurrency Design

1. **One token in flight per ring**  
   Each ring uses an `AtomicBoolean` (`tokenInFlight`) to ensure at most one token circulates at a time. When a token completes, `onTokenComplete()` clears the flag and calls `tryInject()` to start the next pending token if any.

2. **FIFO queueing**  
   Additional inputs are stored in a `BlockingQueue` (a `LinkedBlockingQueue`) to preserve FIFO ordering. When the ring is idle, the next token is taken from this queue and injected.

3. **Message passing between nodes**  
   Each node has an inbox (`BlockingQueue<Token>`). Node `i` forwards tokens to node `(i+1) % N`, forming a directed cycle. Blocking queues avoid busy-waiting and provide thread-safe FIFO semantics.

4. **Completion reporting**  
   When `remaining_hops` reaches 0, the node reports completion to the Coordinator via a `CompletionReporter` callback. A dedicated printer thread consumes from a completion queue so the main input loop does not block on I/O.

5. **Clean shutdown**  
   When the user types `"done"`, the Coordinator requests shutdown on each ring. Rings finish all queued and in-flight work first. When a ring is idle and its pending queue is empty, it injects a poison-pill token; each node forwards the poison to the next and exits, stopping all node threads.

## Design Rationale (for README / Analysis)

### Why BlockingQueue?

`BlockingQueue` is used for both node inboxes and the pending queue because it:
- Is thread-safe
- Guarantees FIFO ordering
- Blocks on `take()` when empty instead of busy-waiting
- Fits the model of tokens flowing between nodes

### Why a separate printer thread?

Completion messages are printed from a dedicated thread that reads from a completion queue. This lets the main thread keep accepting input without blocking on printing, and avoids slowing the rings with I/O.

### Why synchronize tryInject?

`tryInject()` is `synchronized` to avoid race conditions when:
- A token completes (clearing `tokenInFlight`) and
- Another thread might try to inject at the same time.

Without synchronization, two threads could both see `tokenInFlight == false` and both inject, violating the single-token invariant.

### Routing rules

| Input X | Ring |
|---------|------|
| X < 0 | NEG |
| X == 0 | ZERO |
| X > 0 and even | POS_EVEN |
| X > 0 and odd | POS_ODD |

### Ring transformations (64-bit signed arithmetic, overflow allowed)

| Ring | Transformation |
|------|----------------|
| NEG | `v := v * 3 + 1` |
| ZERO | `v := v + 7` |
| POS_EVEN | `v := v * 101` |
| POS_ODD | `v := v * 101 + 1` |

## Usage

```bash
javac *.java
java Main <N> <H>
```

- **N**: Ring size (nodes per ring), N ≥ 1  
- **H**: Hops per token, H ≥ 1  

Example: `java Main 1000 5000` — 1000 nodes per ring, 5000 hops per token.

## Correctness

The implementation satisfies the specification:

- Routes inputs correctly by value  
- Executes exactly H hops per token  
- Enforces FIFO ordering per ring  
- Allows at most one token in flight per ring  
- Avoids deadlock (no circular waits; BlockingQueue handles blocking)  
- Shuts down cleanly after `"done"`  
- Terminates all node and helper threads correctly  
