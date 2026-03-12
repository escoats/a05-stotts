# Four Rings

Team: Elizabeth Coats, Manasi Chaudhary, Emma Coye

## Overview
This project implements a concurrent system that is made up of four rings of nodes. Each ring processes a token derived from user input; this system was implemented in both Elixir and Java. 

A central coordinator reads integer inputs from the user and calls one of the four rings based on input value: 
- NEG: for inputs less than 0
- ZERO: for inputs equal to 0
- POS_EVEN: for inputs that are positive, even numbers
- POS_ODD: for inputs that are positive, odd numbers

Each ring contains N nodes arranged in a directed cycle, so when an input is given the token is made and circulated through ther ing for H hops. Each node will apply the specific transformation to the value, and once the hop count is 0 the final result is given back to the coordinator and then printed.
Rings process one token at a time.

## Elixir Implementation

The Elixir implementation uses spawned processes to represent nodes in each ring.

To enforce that only one token may be active in each ring at a time, we employ additional "gatekeeper" functions. Each gatekeeper has a `receive` block that is sent messages from the main coordinator function `start/2`, and a nested `receive` block within that waits for a confirmation that the ring has finished processing its input. The first block passes an input into the ring, and the second acts as a blocker so that the next token can only enter the ring once the previous token has completed.


## Java Implementation 

The Java implementation uses threads and BlockingQueue for message passing between nodes. 
Each onde runs as a thread and forwards tokens via thread-safe queues. The ring manager enforces FIFO queueing and the one-token rule. 