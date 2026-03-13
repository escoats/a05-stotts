# Analysis

## System Setup

All experiments were run on a MacBook Air with an Apple M-series processor and 16GB of RAM.

## Parameters and effects

The two main parameters tested were N, the number of nodes in each ring, and H, the number of hops each token makes through the ring. Increasing N increases the number of concurrent processes or threads that have to be created and managed. Increasing H increases the amount of computation performed per token. To compare the behavior of the two implementations in Elixir and Java, we ran both versions using the same N and H values and observed differences in startup times, responsiveness, and resource usage.

We chose these two parameters because we want to see how the implementations work under different types of workloads — specifically, concurrency (how many nodes are running at once) and computation (how much work is actually being done with each token). To compare how these different parameters work, we use the behaviors in both implementations to see the differences in either start times, usage, and its responsiveness.

## Observations

When increasing the ring size N, the Elixir implementation could handle a large number of nodes with relatively little performance impact. Elixir processes on the BEAM virtual machine are lightweight, meaning that creating multiple processes doesn't significantly slow down the system.

On the other hand, the Java implementation makes a separate thread for each node. This requires more memory and overhead scheduling. The Java version slows down as the number of nodes increases.

Increasing the hop count increased the amount of computation performed by each token (more transformations had to be applied as the token moved through the ring). Both implementation versions handled increases in H similarly, since the workload became bound by CPU rather than by process management.

## Surprising Observations

One surprising observation that we made was during the testing of very large ring sizes. With N = 50,000, the Java implementation failed due to its OS thread limits, while the Elixir implementation continued to run normally. This also highlights the advantages and scalability of BEAM processes in Elixir compared to Java’s heavier OS-level threads, as mentioned previously. On the other hand, when we set H = 50,000, both implementations ran smoothly, which we expected, though we anticipated some performance change to some extent. This suggests that increasing computation alone did not create the same kinds of memory and scalability issues we saw with nodes.

Though not directly related to performance, we also noted that there was significantly more code involved when implementing the program specifications in Java (four separate files, as compared to a single, hundred-line file in Elixir). Message-sending is native to Elixir and already ordered with FIFO queues, which made implementation simpler. Given that we also observed better large-scale performance in Elixir, we came away feeling that Elixir is a better choice than Java for highly concurrent applications.
