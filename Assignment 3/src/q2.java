import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class q2 {
    static class Parameters {
        boolean b;
        int f;
        int m;

        public Parameters(boolean b, int f, int m) {
            this.b = b;
            this.f = f;
            this.m = m;
        }

        static Parameters concatenate(Parameters left, Parameters right) {
            boolean b = (left.b && right.b) || ((left.f + right.f == 0) && (left.m >= 0) && (left.f + right.m >= 0));
            int f = left.f + right.f;
            int m = Math.min(left.m, left.f + right.m);

            return new Parameters(b, f, m);
        }

        static Parameters parameterize(char[] string) {
            boolean b = true;
            int f = 0;
            int m = 0;

            for (char c : string) {
                if (c == '[') {
                    f++;
                } else if (c == ']') {
                    f--;
                }
                m = Math.min(m, f);
            }
            if (f == 0 && m == 0) {
                b = true;
            } else {
                b = false;
            }
            return new Parameters(b, f, m);
        }
    }

    // Parameters
    private static int n; // String length
    private static int t; // number of threads
    private static long s; // optional seed

    public static void main(String[] args) {
        // Read parameters
        if (args.length < 2) {
            System.out.println("Usage: q2.java n t s");
            return;
        }
        n = Integer.parseInt(args[0]);
        t = Integer.parseInt(args[1]);
        s = (args.length > 2) ? Long.parseLong(args[2]) : System.currentTimeMillis();

        char[] string = Bracket.construct(n, s);

        ExecutorService threadPool = Executors.newFixedThreadPool(t);

        long startTime = System.currentTimeMillis();
        Parameters parameters = null;
        try {
            parameters = new q2().run(threadPool, string, t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();

        threadPool.shutdown();

        long duration = endTime - startTime;
        System.out.println("Total time taken to bracket match string of length " + n + " with " + t + " threads : " + duration + "ms");

        // Compare parallel parameters with sequential Bracket.verify()
        assert parameters != null;
        boolean concurrentParams = parameters.b;
        boolean sequentialParams = Bracket.verify();
        System.out.println("My result: " + concurrentParams);
        System.out.println("Sequential result: " + sequentialParams);
    }

    private Parameters run(ExecutorService threadPool, char[] string, int numThreads) throws ExecutionException, InterruptedException {
        // Divide string into substring by number of threads
        char[][] subStrings = splitString(string, numThreads);

        List<Parameters> subStringParameterizations = new ArrayList<>(numThreads);

        List<Future<Parameters>> subStringParameterizationResults = new ArrayList<>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            int finalI = i;
            subStringParameterizationResults.add(threadPool.submit(() -> Parameters.parameterize(subStrings[finalI])));
        }

        // Gather Parameters from subStrings
        for (Future<Parameters> result : subStringParameterizationResults) {
            try {
                subStringParameterizations.add(result.get());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Combine Parameters
        while (subStringParameterizations.size() > 1) {
            List<Future<Parameters>> futures = new ArrayList<>();
            List<Parameters> newParameters = new ArrayList<>();

            for (int i = 0; i < subStringParameterizations.size(); i += 2) {
                if (i + 1 < subStringParameterizations.size()) {
                    // Submit parallel tasks to merge pairs of subStringParameterizations
                    final Parameters left = subStringParameterizations.get(i);
                    final Parameters right = subStringParameterizations.get(i + 1);
                    futures.add(threadPool.submit(() -> Parameters.concatenate(left, right)));
                } else {
                    // If there's an odd element, just carry it forward
                    newParameters.add(subStringParameterizations.get(i));
                }
            }

            // Gather subStringParameterizations from futures
            for (Future<Parameters> future : futures) {
                newParameters.add(future.get()); // Blocking call, waits for each future to complete
            }

            subStringParameterizations = newParameters; // Replace with reduced results
        }

        return subStringParameterizations.getFirst();
    }

    private static char[][] splitString(char[] string, int numSubStrings) {
        if (numSubStrings > string.length) {
            char[][] result = new char[string.length][];

            for (int i = 0; i < string.length; i++) {
                result[i][0] = string[i];
            }
            return result;
        }

        char[][] result = new char[numSubStrings][];

        int partLength = (int) Math.ceil((double) string.length / numSubStrings);
        int start = 0;
        int end = 0;
        for (int i = 0; i < numSubStrings; i++) {
            end = Math.min(start + partLength, string.length);
            result[i] = new char[end - start];
            System.arraycopy(string, start, result[i], 0, end - start);
            start = end;
        }
        return result;
    }
}
