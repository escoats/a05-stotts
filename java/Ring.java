import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ring.java - Manages one of the four rings: nodes, queueing, and token injection.
 */
public class Ring {
    private final String ringId;
    private final RingType ringType;
    private final int n;
    private final int h;
    private final BlockingQueue<Token> pendingQueue;
    private final BlockingQueue<Token>[] nodeInboxes;
    private final Thread[] nodeThreads;
    private final CompletionReporter completionReporter;
    private final AtomicBoolean tokenInFlight;
    private volatile boolean shutdownRequested;
    private volatile boolean poisonSent;

    public Ring(String ringId, RingType ringType, int n, int h,
                CompletionReporter completionReporter) {
        this.ringId = ringId;
        this.ringType = ringType;
        this.n = n;
        this.h = h;
        this.pendingQueue = new LinkedBlockingQueue<>();
        this.nodeInboxes = new BlockingQueue[n];
        this.nodeThreads = new Thread[n];
        this.completionReporter = completionReporter;
        this.tokenInFlight = new AtomicBoolean(false);
        this.shutdownRequested = false;

        // dir cycle for node in section 5
        for (int i = 0; i < n; i++) {
            nodeInboxes[i] = new LinkedBlockingQueue<>();
        }
        for (int i = 0; i < n; i++) {
            BlockingQueue<Token> nextInbox = nodeInboxes[(i + 1) % n];
            RingNode node = new RingNode(nodeInboxes[i], nextInbox, ringType, this, i);
            nodeThreads[i] = new Thread(node, ringId + "-node-" + i);
        }
    }

    public void start() {
        for (Thread t : nodeThreads) {
            t.start();
        }
    }

    // fifo section 9.
    public void submit(Token token) {
        pendingQueue.offer(token);
        tryInject();
    }

    void onTokenComplete(Token token) {
        completionReporter.report(token);
        tokenInFlight.set(false);
        tryInject();
    }

    private synchronized void tryInject() {
        if (tokenInFlight.get()) return;
        Token next = pendingQueue.poll();
        if (next != null) {
            tokenInFlight.set(true);
            nodeInboxes[0].offer(next);
        } else if (shutdownRequested && !poisonSent) {
            poisonSent = true;
            nodeInboxes[0].offer(RingNode.POISON);
        }
    }

    public void requestShutdown() {
        shutdownRequested = true;
        tryInject();
    }

    public void awaitShutdown() throws InterruptedException {
        for (Thread t : nodeThreads) {
            t.join();
        }
    }

    public int getPendingCount() {
        return pendingQueue.size();
    }

    public boolean isIdle() {
        return !tokenInFlight.get();
    }

    public interface CompletionReporter {
        void report(Token token);
    }
}
