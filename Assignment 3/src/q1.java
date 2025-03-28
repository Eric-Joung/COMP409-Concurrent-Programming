import java.lang.reflect.Array;
import java.util.Random;

public class q1 {
    // Parameters
    private static int m; // number of operations;
    private static int k; // odds of accessing one past end
    private static final int NUM_THREADS = 4;
    private static final Random rand = new Random();

    // Interface for the different arrays
    public interface ConcurrentArray {
        Object get (int i);
        void set(int i, Object o);
        int length();
    }

    public static void main(String[] args) {
        // Read parameters
        if (args.length != 2) {
            System.out.println("Usage: q1.java k m");
            return;
        }
        k = Integer.parseInt(args[0]);
        m = Integer.parseInt(args[1]);

        // Setup tests
        q1a blockingArray = new q1a();
        q1b atomicArray = new q1b();

        ArrayTester[] threadsBlocking = new ArrayTester[NUM_THREADS];
        ArrayTester[] threadsAtomic = new ArrayTester[NUM_THREADS];

        try {
            /* Blocking */
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < NUM_THREADS; i++) {
                threadsBlocking[i] = new ArrayTester(blockingArray);
                threadsBlocking[i].start();
            }

            for (ArrayTester arrayTester : threadsBlocking) {
                arrayTester.join();
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("Total time taken to perform " + m + " operations with " + k + " percent resizing for blocking: " + duration + " ms");

            /* Atomic */
            startTime = System.currentTimeMillis();

            for (int i = 0; i < NUM_THREADS; i++) {
                threadsAtomic[i] = new ArrayTester(atomicArray);
                threadsAtomic[i].start();
            }

            for (ArrayTester arrayTester : threadsAtomic) {
                arrayTester.join();
            }

            endTime = System.currentTimeMillis();
            duration = endTime - startTime;

            System.out.println("Total time taken to perform " + m + " operations with " + k + " percent resizing for blocking: " + duration + " ms");

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

    static class ArrayTester implements Runnable {
        private Thread thread;
        ConcurrentArray concurrentArray;

        public ArrayTester(ConcurrentArray concurrentArray) {
            this.concurrentArray = concurrentArray;
        }

        @Override
        public void run() {
            int numOperationsCompleted = 0;
            while (numOperationsCompleted < m) {
                int odd = rand.nextInt(100);
                if (odd < 99 - k) {
                    readOrWriteExistingElement();
                }
                else {
                    readOrWriteOnePastElement();
                }
                numOperationsCompleted++;
            }
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        public void join() throws InterruptedException {
            thread.join();
        }

        public void readOrWriteExistingElement() {
            if (rand.nextInt(2) == 0) {
                concurrentArray.get(rand.nextInt(concurrentArray.length()));
            }
            else {
                concurrentArray.set(rand.nextInt(concurrentArray.length()), new Object());
            }
        }

        public void readOrWriteOnePastElement() {
            if (rand.nextInt(2) == 0) {
                concurrentArray.get(concurrentArray.length());
            }
            else {
                concurrentArray.set(concurrentArray.length(), new Object());
            }
        }

    }
}
