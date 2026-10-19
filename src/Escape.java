import java.util.Arrays;

/**
 * Mission 3. Escape: DFS from cell 0 to the last cell (rows * cols - 1) in the maze of Mission 1.
 * Two neighbouring cells are connected when the wall between them is broken.
 * The neighbours are tried in this order: up, right, down, left (cell - cols, cell + 1, cell + cols, cell - 1).
 * The ready method Graphs.open(w, broken) gives you the open neighbours of every cell, in no particular order:
 * to visit them in the required order, go through the four cells above one by one and take those that are in the list.
 * The program must not overflow the stack on a maze of 1000 by 1000 cells.
 */
public class Escape {

    /**
     * path: the cells of the way out, from 0 to the last cell.
     * visited: how many cells are marked as visited at the moment the exit is marked (the start and the exit are counted).
     * deadEnds: how many cells of the maze have exactly one open neighbour.
     */
    public record Result(int[] path, int visited, int deadEnds) {}

    public static Result escape(Input.Walls w, int[] broken) {
        int n = w.cells();
        int[][] open = Graphs.open(w, broken);
        int deadEnds = 0;
        for (int[] neighbours : open) {
            if (neighbours.length == 1) deadEnds++;
        }
        if (n == 0) return new Result(new int[0], 0, deadEnds);

        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        byte[] nextDirection = new byte[n];
        int top = 0;
        stack[0] = 0;
        seen[0] = true;
        int visited = 1;

        while (top >= 0) {
            int cell = stack[top];
            // The active DFS stack is the path from the start to this cell.
            if (cell == n - 1) {
                return new Result(Arrays.copyOf(stack, top + 1), visited, deadEnds);
            }
            if (nextDirection[top] == 4) {
                top--;
                continue;
            }

            // Each frame remembers its next direction: up, right, down, left.
            int direction = nextDirection[top]++;
            int next = -1;
            if (direction == 0 && cell >= w.cols()) next = cell - w.cols();
            if (direction == 1 && cell % w.cols() < w.cols() - 1) next = cell + 1;
            if (direction == 2 && cell + w.cols() < n) next = cell + w.cols();
            if (direction == 3 && cell % w.cols() > 0) next = cell - 1;
            if (next == -1 || seen[next]) continue;

            boolean canWalk = false;
            for (int neighbour : open[cell]) {
                if (neighbour == next) {
                    canWalk = true;
                    break;
                }
            }
            if (canWalk) {
                seen[next] = true;
                visited++;
                stack[++top] = next;
                nextDirection[top] = 0;
            }
        }
        return new Result(new int[0], visited, deadEnds);
    }
}
