import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class q1 {
    // parameters
    private static int s;
    private static int n;
    private static int t;
    private static int k;

    // data structures
    private List<CharFrequency> charFrequencies = new ArrayList<>();

    public static void main(String[] args) {
        try {

            BufferedReader reader = new BufferedReader(new FileReader("freq.txt"));

            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Random random = new Random(s);

        n = 5;

        Cell[][] puzzleGrid = new Cell[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                puzzleGrid[i][j] = new Cell('a');
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(puzzleGrid[i][j].character);
            }
            System.out.println();
        }

    }

    private static char generateCharacter(int r) {
        return 'a';
    }


    private static class Cell {
        private char character;
        private HashSet<String> WordContributions;

        public Cell(char character) {
            this.character = character;
            this.WordContributions = new HashSet<>();
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
