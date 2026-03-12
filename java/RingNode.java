import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * RingNode.java - A single node in a ring; runs as its own thread.
 *
 * REASONING: Each ring has N nodes arranged in a directed cycle. A node receives
 * tokens via its inbox (BlockingQueue), applies the ring-specific transformation,
 * decrements remaining_hops, and either forwards to the next node or reports
 * completion. We use BlockingQueue for message passing because it provides
 * thread-safe FIFO semantics and blocks when empty (no busy-waiting). The
 * POISON token signals shutdown so we can terminate cleanly when the user types
 * "done".
 */
public class RingNode implements Runnable {
    // REASONING: Poison-pill pattern for clean shutdown. When the ring is idle and
    // shutdown is requested, the Ring injects this sentinel. Each node forwards it
    // to the next and exits, allowing all node threads to terminate (Section 10).
    public static final Token POISON = new Token(-1, "SHUTDOWN", -1, -1);

    private final BlockingQueue<Token> inbox;
    private final BlockingQueue<Token> nextInbox;
    private final RingType ringType;
    private final Ring ring;
    private final int nodeIndex;

    public RingNode(BlockingQueue<Token> inbox,
                    BlockingQueue<Token> nextInbox,
                    RingType ringType,
                    Ring ring,
                    int nodeIndex) {
        this.inbox = inbox;
        this.nextInbox = nextInbox;
        this.ringType = ringType;
        this.ring = ring;
        this.nodeIndex = nodeIndex;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Token token = inbox.take();
                if (token == POISON) {
                    nextInbox.offer(POISON);
                    return;
                }
                // REASONING: Hop processing per Section 7: (1) apply ring transform,
                // (2) decrement remaining_hops, (3) forward if >0 else report completion.
                token.currentVal = ringType.transform(token.currentVal);
                token.remainingHops--;
                if (token.remainingHops > 0) {
                    nextInbox.put(token);
                } else {
                    ring.onTokenComplete(token);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
