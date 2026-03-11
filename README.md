# Four Rings

Team: Elizabeth Coats, Manasi Chaudhary, Emma Coye

## Elixir Implementation

The Elixir implementation uses spawned processes to represent nodes in each ring.

To enforce that only one token may be active in each ring at a time, we employ additional "gatekeeper" functions. Each gatekeeper has a `receive` block that is sent messages from the main coordinator function `start/2`, and a nested `receive` block within that waits for a confirmation that the ring has finished processing its input. The first block passes an input into the ring, and the second acts as a blocker so that the next token can only enter the ring once the previous token has completed.
