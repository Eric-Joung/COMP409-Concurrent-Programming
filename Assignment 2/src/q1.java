import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class q1 {
    // parameters
    private static int s = 10;
    private static int n = 5;
    private static int t = 3;
    private static int k = 50;

    // data structures
    private static Cell[][] puzzleGrid = new Cell[n][n];
    private static List<CharFrequency> charFrequencies = new ArrayList<>();
    private static HashSet<String> dictionary = new HashSet<>();
    private static final Random random = new Random(s);
    private static final int[][] DIRECTIONS = {
            {-1, 0}, // UP
            { 1,  0}, // Down
            { 0, -1}, // Left
            { 0,  1}, // Right
            {-1, -1}, // Top-Left
            {-1,  1}, // Top-Right
            { 1, -1}, // Bottom-Left
            { 1,  1}  // Bottom-Right
    };

    public static void main(String[] args) {
        // Try to read from files
        try {
            // Read frequency of letters
            BufferedReader freqReader = new BufferedReader(new FileReader("freq.txt"));

            String freqLine;
            int cumulative = 0;
            while ((freqLine = freqReader.readLine()) != null) {
                // If line is empty, the file does not need to be read anymore
                if (freqLine.isEmpty()) {
                    break;
                }

                String[] parts = freqLine.split("\\s+");

                char ch = parts[0].charAt(0);
                int frequency = Integer.parseInt(parts[1]);
                cumulative += frequency;

                charFrequencies.add(new CharFrequency(ch, cumulative));
            }

            // Read dict and store in memory
            BufferedReader dictReader = new BufferedReader(new FileReader("dict.txt"));

            String dictLine;
            while((dictLine = dictReader.readLine()) != null) {
                dictionary.add(dictLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize puzzleGrid
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int randValue = random.nextInt(100000);
                puzzleGrid[i][j] = new Cell(generateCharacter(randValue), i, j);
            }
        }

        // Print out the puzzle grid
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(puzzleGrid[i][j].character);
            }
            System.out.println();
        }

        try {
            // Start the threads
            WordFinder[] threads = new WordFinder[t];
            for (int i = 0; i < t; i++) {
                threads[i] = new WordFinder();
                threads[i].start();
            }

            // Wait for threads to finish
            for (WordFinder wordFinder : threads) {
                wordFinder.join();
            }
        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }

        // Print out the contributions of each Cell
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                puzzleGrid[i][j].printWordContributions();
            }
        }
    }

    private static char generateCharacter(int randValue) {
        for (CharFrequency cf : charFrequencies) {
            if (randValue <= cf.cumulativeFrequency) {
                return cf.character;
            }
        }

        return '5';
    }

    private static class WordFinder implements Runnable {
        private Thread thread;
        private int numSequences = k;
        private List<Cell> cellSequence = new ArrayList<>();
        private List<Lock> acquiredCellLocks = new ArrayList<>();
        private static final Object lock = new Object();

        public void run() {
            try {
                while (numSequences > 0) {
                    pickStartingCell();
                    pickSequenceCell();
                    while (cellSequence.size() < 7) {
                        int odds = random.nextInt(5);
                        if (odds <= 0) break;
                        // Try to pick the next cell in the sequence, if none valid -> break
                        if (!pickSequenceCell()) {
                            break;
                        }
                    }


                    // Try to acquire all locks in order. If fail, sleep and try again
                    if (!acquireSequenceLocks()) {
                        Thread.sleep(10);
                    }

                    // Explore the sequence
                    for (int i = 1; i < cellSequence.size(); i++) {
                        char[] seq = new char[i + 1];
                        for (int j = 0; j <= i; j++) {
                            seq[j] = cellSequence.get(j).character;
                        }

                        // Check if sequence is an existing word, and add the word to each list of each cell
                        String sequence = new String(seq).toLowerCase();
                        if (dictionary.contains(sequence)) {
                            for (int k = 0; k <= i; k++) {
                                cellSequence.get(k).wordContributions.add(sequence);
                            }
                        }
                    }

                    // Once finished, clear all elements in cellSequence and sleep before starting again
                    cellSequence.clear();
                    for (Lock lock : acquiredCellLocks) {
                        lock.unlock();
                    }
                    acquiredCellLocks.clear();
                    numSequences--;

                    Thread.sleep(20);
                }
            } catch (Exception e) {
                System.out.println("Exception in thread:" + e);
                e.printStackTrace();
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

        private void pickStartingCell() {
            int index = random.nextInt(n*n);
            int row = index / n;
            int column;
            if (index == 0) {
                column = 0;
            }
            else {
               column = (index - 1) % n;
            }

            cellSequence.add(puzzleGrid[row][column]);
        }

        private boolean pickSequenceCell() {
            Cell currentCell = cellSequence.getLast();
            // Check all 8 possible directions
            List<Cell> validMoves = new ArrayList<>();

            for (int[] direction : DIRECTIONS) {
                int newRow = currentCell.row + direction[0];
                int newCol = currentCell.column + direction[1];

                // Check if move is valid
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n) {
                    Cell potentialCell = puzzleGrid[newRow][newCol];
                    if (!cellSequence.contains(potentialCell)) {
                        validMoves.add(potentialCell);
                    }
                }
            }

            if (validMoves.isEmpty()) {
                return false;
            }

            cellSequence.add(validMoves.get(random.nextInt(validMoves.size())));
            return true;
        }

        private synchronized boolean acquireSequenceLocks() {
            try {
                for (Cell cell : cellSequence) {
                    if (cell.lock.tryLock()) {
                        acquiredCellLocks.add(cell.lock);
                    }
                    else {
                        if (!acquiredCellLocks.isEmpty()) {
                            for (Lock lock : acquiredCellLocks) {
                                lock.unlock();
                            }
                            acquiredCellLocks.clear();
                        }
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                acquiredCellLocks.clear();
                return false;
            }
        }
    }


    private static class Cell {
        private final ReentrantLock lock = new ReentrantLock();
        private final char character;
        private HashSet<String> wordContributions = new HashSet<>();
        private final int row;
        private final int column;

        public Cell(char character, int row, int column) {
            this.character = character;
            this.row = row;
            this.column = column;
        }

        public void printWordContributions() {
            System.out.print(row + "," + column + " ");
            wordContributions.forEach(word -> System.out.print(word + " "));
            System.out.println();
        }
    }

    private static class CharFrequency {
        char character;
        int cumulativeFrequency;

        public CharFrequency(char character, int cumulativeFrequency) {
            this.character = character;
            this.cumulativeFrequency = cumulativeFrequency;
        }

    }
}
