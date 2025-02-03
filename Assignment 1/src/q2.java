import java.util.HashSet;

public class q2 {

    private static Cell[][] gameGrid;

    private static HashSet<Portal> portals = new HashSet<>();

    public static void main(String[] args) {

        gameGrid = new Cell[10][10];
        for (int i = 0; i < gameGrid.length; i++) {
            gameGrid[i] = new Cell(i);
        }

        try {

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

}

class Cell {

    public Portal portalPointer;

    public Cell() {
    }

}

abstract class Portal {
    public Cell head;

    public Cell tail;
}

class Snake extends Portal {

}

class Ladder extends Portal{

}
