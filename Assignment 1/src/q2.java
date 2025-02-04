import java.util.HashSet;
import java.util.Random;

import static java.lang.Thread.sleep;

public class q2 {

    // Parameters
    private static int k;
    private static int j;
    private static int s;

    private static Cell[][] gameGrid;

    private static HashSet<Portal> portals = new HashSet<>();

    private static final Random rand = new Random();

    private static long startTime;

    public static void main(String[] args) {

        gameGrid = new Cell[10][10];
        int currentPosition = 0;
        for (int i = 0; i < 10; i++) {
            {
                for (int j = 0; j < 10; j++) {
                    gameGrid[i][j] = new Cell(null, currentPosition++);
                }
            }
        }

        // Draw the grid
        for (Cell[] rows : gameGrid) {
            for (Cell cell : rows) {
                System.out.print("[" + cell.position + "]");
            }
            System.out.println("");
        }

//        // Instantiate Snakes
//        for (int i = 0; i < 10; i ++) {
//            int headCellPosition = rand.nextInt(99 - 10 + 1) + 10;
//            int tailCellPosition = rand.nextInt(headCellPosition - 1 + 1) + 1;
//            Snake snake = new Snake()
//        }
//
//        // Instantiate Ladders
//        for (int i = 0;  i < 9; i++) {
//
//        }

        try {
            startTime = System.currentTimeMillis();

            Player player = new Player();
            player.start();

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

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
                while (true) {
                    int finish = rollDice();
                    long endTime = System.currentTimeMillis();
                    System.out.println(endTime - startTime + " Player " + currentCell.position);
                    if (finish == 1) {
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
    }

    static class SnakeGenerator implements Runnable {

        public void run() {

        }

        public void start() {

        }
    }

    static class LadderGenerator implements Runnable {

        public void run() {

        }
    }

    static class Cell {

        public Portal portalPointer;
        public int position;

        public Cell(Portal portal, int position) {
            this.portalPointer = portal;
            this.position = position;
        }

    }

    abstract static class Portal {
        public Cell head;

        public Cell tail;
    }

    static class Snake extends Portal {

        public Snake(Cell headCell, Cell tailCell) {
            this.head = headCell;
            this.tail = tailCell;
        }
    }

    static class Ladder extends Portal {

        public Ladder(Cell headCell, Cell tailCell) {
            this.head = headCell;
            this.tail = tailCell;
        }

    }
}




