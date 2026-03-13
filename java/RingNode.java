import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * RingNode.java - A single node in a ring; runs as its own thread.
 */
public class RingNode implements Runnable {
    // Poison-pill pattern for clean shutdown.
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
                // Hop processing for section 7
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
