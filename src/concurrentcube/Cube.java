package concurrentcube;

import java.util.function.BiConsumer;

public class Cube {
    // [side][row][column]
    private int size;
    private int[][][] squares;
    private BiConsumer<Integer, Integer> beforeRotation;
    private BiConsumer<Integer, Integer> afterRotation;
    private Runnable beforeShowing;
    private Runnable afterShowing;

    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing) {
        this.size = size;
        this.squares = new int[6][size][size];
        for (int side = 0; side < 6; side++) {
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    squares[side][row][column] = side;
                }
            }
        }

        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;
    }

    private int[] copyRow(int side, int row) {
        int[] copy = new int[size];
        for (int column = 0; column < size; column++) {
            copy[column] = squares[side][row][column];
        }
        return copy;
    }

    private int[] copyColumn(int side, int column) {
        int[] copy = new int[size];
        for (int row = 0; row < size; row++) {
            copy[row] = squares[side][row][column];
        }
        return copy;
    }

    private int[][] copySide(int side) {
        int[][] copy = new int[size][size];
        for (int row = 0; row < size; row++) {
            copy[row] = copyRow(side, row);
        }
        return copy;
    }

    private void rewriteColumn(int sideFrom, int sideTo, int columnFrom, int columnTo) {
        for (int row = 0; row < size; row++) {
            squares[sideTo][row][columnTo] = squares[sideFrom][row][columnFrom];
        }
    }

    private void rewriteColumnConversely(int sideFrom, int sideTo, int columnFrom, int columnTo) {
        for (int row = 0; row < size; row++) {
            squares[sideTo][row][columnTo] = squares[sideFrom][size - 1 - row][columnFrom];
        }
    }

    private void rewriteRow(int sideFrom, int sideTo, int rowFrom, int rowTo) {
        for (int column = 0; column < size; column++) {
            squares[sideTo][rowTo][column] = squares[sideFrom][rowFrom][column];
        }
    }

    private void rewriteRowConversely(int sideFrom, int sideTo, int rowFrom, int rowTo) {
        for (int column = 0; column < size; column++) {
            squares[sideTo][rowTo][column] = squares[sideFrom][rowFrom][size - 1 - column];
        }
    }

    private void rewriteRowToColumn(int sideFrom, int sideTo, int rowFrom, int columnTo) {
        for (int row = 0; row < size; row++) {
            squares[sideTo][row][columnTo] = squares[sideFrom][rowFrom][row];
        }
    }

    private void rewriteRowToColumnConversely(int sideFrom, int sideTo, int rowFrom, int columnTo) {
        for (int row = 0; row < size; row++) {
            squares[sideTo][row][columnTo] = squares[sideFrom][rowFrom][size - 1 - row];
        }
    }

    private void rewriteColumnToRow(int sideFrom, int sideTo, int columnFrom, int rowTo) {
        for (int column = 0; column < size; column++) {
            squares[sideTo][rowTo][column] = squares[sideFrom][column][columnFrom];
        }
    }

    private void rewriteColumnToRowConversely(int sideFrom, int sideTo, int columnFrom, int rowTo) {
        for (int column = 0; column < size; column++) {
            squares[sideTo][rowTo][column] = squares[sideFrom][size - 1 - column][columnFrom];
        }
    }

    // right rotates only top squares on the side
    // this operation is only for rotate(side, 0)
    private void rightRotateSide(int side) {
        int[][] copySide =  copySide(side);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++)
                squares[side][row][column] = copySide[size - 1 - column][row];
        }
    }

    // left rotates only top squares on the side
    // this operation is only for rotate(side, size - 1)
    private void leftRotateSide(int side) {
        int[][] copySide =  copySide(side);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++)
                squares[side][row][column] = copySide[column][size - 1 - row];
        }
    }

    // rotates layer looking from side 0
    // (without squares on side 0 and side 5)
    private void rotatePerimeterLayer0(int layer) {
        int[] rowTemp = copyRow(1, layer);
        for (int i = 1; i < 4; i++) {
            rewriteRow(i + 1, i, layer, layer);
        }
        squares[4][layer] = rowTemp;
    }

    // rotates layer looking from side 1
    // (without squares on side 1 and side 3)
    private void rotatePerimeterLayer1(int layer) {
        int[] columnTemp = copyColumn(0, layer);
        rewriteColumnConversely(4, 0, size - 1 - layer, layer);
        rewriteColumnConversely(5, 4, layer, size - 1 - layer);
        rewriteColumn(2, 5, layer, layer);
        for (int row = 0; row < size; row++) {
            squares[2][row][layer] = columnTemp[row];
        }
    }

    // rotates layer looking from side 2
    // (without squares on side 2 and side 4)
    private void rotatePerimeterLayer2(int layer) {
        int[] rowTemp = copyRow(0, size - 1 - layer);
        rewriteColumnToRowConversely(1, 0, size - 1 - layer, size - 1 - layer);
        rewriteRowToColumn(5, 1, layer, size - 1 - layer);
        rewriteColumnToRowConversely(3, 5, layer, layer);
        for (int row = 0; row < size; row++) {
            squares[3][row][layer] = rowTemp[row];
        }
    }

    // rotates layer looking from side 3
    // (without squares on side 1 and side 3)
    private void rotatePerimeterLayer3(int layer) {
        int[] columnTemp = copyColumn(0, size - 1 - layer);
        rewriteColumn(2, 0, size - 1 - layer, size - 1 - layer);
        rewriteColumn(5, 2, size - 1 - layer, size - 1 - layer);
        rewriteColumnConversely(4, 5, layer, size - 1 - layer);
        for (int row = 0; row < size; row++) {
            squares[4][row][layer] = columnTemp[size - 1 - row];
        }
    }

    // rotates layer looking from side 4
    // (without squares on side 2 and side 4)
    private void rotatePerimeterLayer4(int layer) {
        int[] columnTemp = copyColumn(3, size - 1 - layer);
        rewriteRowToColumnConversely(5, 3, size - 1 - layer, size - 1 - layer);
        rewriteColumnToRow(1, 5, layer, layer);
        rewriteRowToColumnConversely(0, 1, layer, layer);
        squares[0][layer] = columnTemp;
    }

    // rotates layer looking from side 5
    // (without squares on side 0 and side 5)
    private void rotatePerimeterLayer5(int layer) {
        int[] rowTemp = copyRow(4, size - 1 - layer);
        for (int side = 4; side > 1; side++) {
            rewriteRow(side - 1, side, size - 1 - layer, size - 1 - layer);
        }
        squares[1][size - 1 - layer] = rowTemp;
    }
}
