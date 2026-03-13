import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main.java - Coordinator and entry point for the Four Concurrent Rings system.
 */
public class Main {

    public static void main(String[] args) {
        // Section 2 parameters
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

        // Blocking queue for completion messages.
        BlockingQueue<Token> completionQueue = new LinkedBlockingQueue<>();

        Ring.CompletionReporter reporter = token -> completionQueue.offer(token);

        // Section 5 four independent rings.
        Ring negRing = new Ring("NEG", RingType.NEG, n, h, reporter);
        Ring zeroRing = new Ring("ZERO", RingType.ZERO, n, h, reporter);
        Ring posEvenRing = new Ring("POS_EVEN", RingType.POS_EVEN, n, h, reporter);
        Ring posOddRing = new Ring("POS_ODD", RingType.POS_ODD, n, h, reporter);

        negRing.start();
        zeroRing.start();
        posEvenRing.start();
        posOddRing.start();


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

            // "done" stops new input but need to finish the queued/in-flight work
            if (line.equalsIgnoreCase("done")) {
                done = true;
                break;
            }

            long x;
            try {
                x = Long.parseLong(line);
            } catch (NumberFormatException e) {
                // section 3
                System.err.println("Error: could not parse as integer");
                continue;
            }

            long tokenId = nextTokenId.getAndIncrement();

            // routing for section 4
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
