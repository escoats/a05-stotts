import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main.java - Coordinator and entry point for the Four Concurrent Rings system.
 *
 * REASONING: The spec requires a central Coordinator that (1) reads integers from
 * the keyboard, (2) routes each to one of four rings based on value, (3) prints
 * completion messages. We use a dedicated printer thread that reads from a
 * completion queue, so the main thread can focus on input without blocking on
 * I/O. Routing follows the spec: X<0→NEG, X==0→ZERO, X>0 even→POS_EVEN, X>0 odd→POS_ODD.
 * tokenId is a monotonically increasing counter (AtomicLong) for unique identification.
 */
public class Main {

    public static void main(String[] args) {
        // REASONING: N and H are program parameters per the spec (Section 2).
        // N = nodes per ring; H = hops per token. Both must be >= 1.
        if (args.length < 2) {
            System.err.println("Usage: java Main <N> <H>");
            System.err.println("  N = ring size (number of nodes per ring), N >= 1");
            System.err.println("  H = number of hops per token, H >= 1");
            System.exit(1);
        }

        int n = Integer.parseInt(args[0]);
        int h = Integer.parseInt(args[1]);
        if (n < 1 || h < 1) {
            System.err.println("N and H must be >= 1");
            System.exit(1);
        }

        // REASONING: BlockingQueue provides thread-safe FIFO for completion messages.
        // The printer thread blocks on take() until a completion arrives, avoiding
        // busy-waiting. LinkedBlockingQueue is unbounded so we don't drop messages.
        BlockingQueue<Token> completionQueue = new LinkedBlockingQueue<>();

        Ring.CompletionReporter reporter = token -> completionQueue.offer(token);

        // REASONING: Four independent rings run concurrently (Section 5). Each ring
        // has its own RingType for the transformation (Section 8) and CompletionReporter
        // callback for reporting when a token finishes its H hops (Section 7).
        Ring negRing = new Ring("NEG", RingType.NEG, n, h, reporter);
        Ring zeroRing = new Ring("ZERO", RingType.ZERO, n, h, reporter);
        Ring posEvenRing = new Ring("POS_EVEN", RingType.POS_EVEN, n, h, reporter);
        Ring posOddRing = new Ring("POS_ODD", RingType.POS_ODD, n, h, reporter);

        negRing.start();
        zeroRing.start();
        posEvenRing.start();
        posOddRing.start();

        // REASONING: Printer runs in a separate thread so completion messages are
        // printed as soon as they arrive, without blocking the input loop. The
        // POISON_COMPLETE sentinel signals "no more completions" for clean shutdown.
        AtomicLong nextTokenId = new AtomicLong(0);
        Token POISON_COMPLETE = new Token(-1, "DONE", -1, -1);
        Thread printerThread = new Thread(() -> {
            try {
                while (true) {
                    Token t = completionQueue.take();
                    if (t == POISON_COMPLETE) break;
                    System.out.printf("Ring %s completed token_id=%d orig_input=%d final_val=%d%n",
                            t.ringId, t.tokenId, t.origInput, t.currentVal);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "printer");
        printerThread.start();

        Scanner scanner = new Scanner(System.in);
        boolean done = false;

        while (!done) {
            System.out.print("Enter integer (or 'done' to finish): ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();

            // REASONING: "done" stops new input but we must finish all queued/in-flight
            // work before shutting down (Section 3).
            if (line.equalsIgnoreCase("done")) {
                done = true;
                break;
            }

            long x;
            try {
                x = Long.parseLong(line);
            } catch (NumberFormatException e) {
                // REASONING: Per Section 3, if parsing fails we print an error and continue.
                System.err.println("Error: could not parse as integer");
                continue;
            }

            long tokenId = nextTokenId.getAndIncrement();

            // REASONING: Routing rules from Section 4: X<0→NEG, X==0→ZERO,
            // X>0 even→POS_EVEN, X>0 odd→POS_ODD.
            if (x < 0) {
                negRing.submit(new Token(tokenId, "NEG", x, h));
            } else if (x == 0) {
                zeroRing.submit(new Token(tokenId, "ZERO", x, h));
            } else if (x % 2 == 0) {
                posEvenRing.submit(new Token(tokenId, "POS_EVEN", x, h));
            } else {
                posOddRing.submit(new Token(tokenId, "POS_ODD", x, h));
            }
        }

        // REASONING: After "done", we must finish all queued and in-flight work.
        // We request shutdown on each ring (stops new work, signals poison when idle),
        // then wait for all node threads to exit. Finally we poison the completion
        // queue so the printer thread can exit.
        negRing.requestShutdown();
        zeroRing.requestShutdown();
        posEvenRing.requestShutdown();
        posOddRing.requestShutdown();

        try {
            negRing.awaitShutdown();
            zeroRing.awaitShutdown();
            posEvenRing.awaitShutdown();
            posOddRing.awaitShutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        completionQueue.offer(POISON_COMPLETE);

        try {
            printerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scanner.close();
        System.out.println("Shutdown complete.");
    }
}
