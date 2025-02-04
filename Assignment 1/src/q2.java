import java.io.FileWriter;
import java.io.IOException;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.Thread.sleep;

enum Portal {
    LADDER,
    SNAKE;

    private static final Random Random = new Random();

    public static Portal getRandomPortal() {
        Portal[] portals = Portal.values();
        return portals[Random.nextInt(portals.length)];
    }
}

public class q2 {

    // Parameters
    private static int k;
    private static int j;
    private static int s;
    private static final int initialSnakes = 10;
    private static final int initialLadders = 9;
    private static Cell[][] gameGrid;
    private static final List<Integer> portals = new ArrayList<>();
    private static final Object lock = new Object();
    private static final Random rand = new Random();
    private static long startTime;
    private static volatile boolean running = true;
    private static final Queue<String> logQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("Usage: q2.java k j s");
            return;
        }

        k = Integer.parseInt(args[0]);
        j = Integer.parseInt(args[1]);
        s = Integer.parseInt(args[2]) * 1000;

        startTime = System.currentTimeMillis();

        // Initialize board
        gameGrid = new Cell[10][10];
        int currentPosition = 0;
        for (int i = 0; i < 10; i++) {
            {
                for (int j = 0; j < 10; j++) {
                    gameGrid[i][j] = new Cell(null, currentPosition++);
                }
            }
        }

        // Instantiate Snakes
        for (int i = 0; i < initialSnakes; i++) {
            generatePortal(Portal.SNAKE);
        }

        // Instantiate Ladders
        for (int i = 0; i < initialLadders; i++) {
            generatePortal(Portal.LADDER);
        }

        try {
            // Initialize Player, Adder, and Remover threads
            Player player = new Player();
            player.start();

            PortalAdder portalAdder = new PortalAdder();
            portalAdder.start();

            PortalRemover portalRemover = new PortalRemover();
            portalRemover.start();

            long runTime = System.currentTimeMillis();

            // Loop over time and only stop once time exceeds parameter s
            while (runTime - startTime < s) {
                runTime = System.currentTimeMillis();
            }

            // Stop and join all threads
            running = false;
            player.join();
            portalAdder.join();
            portalRemover.join();

            logQueue.add("Program has ended after " + ((runTime - startTime) / 1000) + " seconds");

            writeLogsToFile("Assignment 1/output.txt");
        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

    /**
     * Single threaded implementation of portal generation
     * @param portal - Portal enum
     */
    private static void generatePortal(Portal portal) {
        int headPosition;
        int tailPosition;

        switch (portal) {
            case SNAKE:
                do {
                    headPosition = rand.nextInt(98 - 10 + 1) + 10;
                    tailPosition = rand.nextInt((headPosition / 10 * 10 - 1) - 1 + 1) + 1;

                } while (gameGrid[headPosition / 10][headPosition % 10].portal != null);
                break;
            case LADDER:
                do {
                    tailPosition = rand.nextInt(98 - 10 + 1) + 10;
                    headPosition = rand.nextInt((tailPosition / 10 * 10 - 1) - 1 + 1) + 1;

                } while (gameGrid[headPosition / 10][headPosition % 10].portal != null);
                break;
            default:
                return;
        }

        // Make the found cell point to portal tail
        gameGrid[headPosition / 10][headPosition % 10].portal = gameGrid[tailPosition / 10][tailPosition % 10];
        portals.add(headPosition);
    }

    /**
     * Write contents of ConcurrentLogQueue to filename
     * @param filename - The name of the file to write to
     */
    private static void writeLogsToFile(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            for (String log : logQueue) {
                writer.write(log + "\n");
            }
            writer.close();
            System.out.println("Log file written at " + filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Player class encapsulates player thread
     */
    static class Player implements Runnable {

        private Thread thread;
        private static final int maxRoll = 6;
        private static final int minRoll = 1;

        private Cell currentCell;

        public Player() {
            this.currentCell = gameGrid[0][0];
        }

        public void run() {
            try {
                while (running) {
                    int finish;
                    synchronized (lock) {
                        finish = rollDice();
                        long endTime = System.currentTimeMillis();
                        logQueue.add(endTime - startTime + " Player " + currentCell.position);
                        // if new position is portal head, move to portal tail
                        if (currentCell.portal != null) {
                            int oldCellPosition = currentCell.position;
                            moveToPortalTail();
                            logQueue.add((endTime - startTime) + " Player " + oldCellPosition + " " + currentCell.position);
                        }
                    }
                    if (finish == 1) {
                        logQueue.add("Player won! Restarting...");
                        sleep(1000);
                    } else {
                        sleep(rand.nextInt(50 - 20 + 1) + 20);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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

        /**
         * Stimulates dice roll and player movement given dice roll
         * @return 1 - Player reached finish line 0 - Player did not reach finish line
         * @throws InterruptedException
         */
        private int rollDice() throws InterruptedException {
            int numSteps = rand.nextInt(maxRoll - minRoll + 1) + minRoll;
            int newPosition = numSteps + currentCell.position;
            if (newPosition > 99) {
                currentCell = gameGrid[0][0];
                return 1;
            }
            currentCell = gameGrid[newPosition / 10][newPosition % 10];
            return 0;
        }

        /**
         * Moves player to the tail of the portal
         */
        private void moveToPortalTail() {
            int newPosition = currentCell.portal.position;
            currentCell = gameGrid[newPosition / 10][newPosition % 10];
        }
    }

    /**
     * PortalAdder class encapsulates portalAdder thread
     */
    static class PortalAdder implements Runnable {
        private Thread thread;
        private Integer portalHead;
        private Integer portalTail;
        public void run() {
            try {
                while (running) {
                    Portal portal = Portal.getRandomPortal();
                    generatePortal(portal);
                    sleep(k);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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

        /**
         * Generates either a Ladder or a Snake
         * @param portal - Portal enum
         */
        private static void generatePortal(Portal portal) {
            int headPosition;
            int tailPosition;

            switch (portal) {
                case SNAKE:
                    do {
                        headPosition = rand.nextInt(98 - 10 + 1) + 10;
                        tailPosition = rand.nextInt((headPosition / 10 * 10 - 1) - 1 + 1) + 1;

                    } while (gameGrid[headPosition / 10][headPosition % 10].portal != null);
                    break;
                case LADDER:
                    do {
                        tailPosition = rand.nextInt(98 - 10 + 1) + 10;
                        headPosition = rand.nextInt((tailPosition / 10 * 10 - 1) - 1 + 1) + 1;

                    } while (gameGrid[headPosition / 10][headPosition % 10].portal != null);
                    break;
                default:
                    return;
            }

            // Make the found cell point to portal tail
            synchronized (lock) {
                gameGrid[headPosition / 10][headPosition % 10].portal = gameGrid[tailPosition / 10][tailPosition % 10];
                portals.add(headPosition);
                long endTime = System.currentTimeMillis();
                if (portal == Portal.SNAKE) {
                    logQueue.add((endTime - startTime) + " Adder snake " + tailPosition + " " + headPosition);
                } else {
                    logQueue.add((endTime - startTime) + " Adder ladder " + headPosition + " " + tailPosition);
                }
            }
        }
    }

    /**
     * PortalRemover class encapsulates portalRemover thread
     */
    static class PortalRemover implements Runnable {
        private Thread thread;
        public void run() {
            try {
                while (running) {
                    removePortal();
                    sleep(j);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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

        /**
         * Removes portal
         */
        private static void removePortal() {
            synchronized (lock) {
                int randomPortalIndex = rand.nextInt(portals.size());
                int portalPosition = portals.get(randomPortalIndex);

                Cell cell = gameGrid[portalPosition / 10][portalPosition % 10];
                int headPosition = cell.position;
                int tailPosition = cell.portal.position;
                // Check if portal was snake or ladder
                Portal portal;
                if (headPosition > tailPosition) {
                    portal = Portal.SNAKE;
                } else {
                    portal = Portal.LADDER;
                }
                // Remove pointer to portal tail
                cell.portal = null;


                portals.remove(randomPortalIndex);
                long endTime = System.currentTimeMillis();
                if (portal == Portal.SNAKE) {
                    logQueue.add((endTime - startTime) + " Remover snake " + tailPosition + " " + headPosition);
                } else {
                    logQueue.add((endTime - startTime) + " Remover ladder " + headPosition + " " + tailPosition);
                }
            }
        }
    }

    /**
     * Cell class encapsulates individual grid sections of the game board
     */
    static class Cell {
        public Cell portal;
        public int position;

        public Cell(Cell portal, int position) {
            this.portal = portal;
            this.position = position;
        }




    }
}




