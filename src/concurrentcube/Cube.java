package concurrentcube;

import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;


public class Cube {
    // [side][row][column]
    private int size;
    private int[][][] squares;
    private BiConsumer<Integer, Integer> beforeRotation;
    private BiConsumer<Integer, Integer> afterRotation;
    private Runnable beforeShowing;
    private Runnable afterShowing;

    // informs how many threads are waiting for operations:
    // in each group: rotate() on axis 0, 1 or 2 and show();
    // axis_i crosses side i
    private int[] waitingGroup;
    // informs how many threads are waiting for operation rotate()
    // on each layer
    private int[][] waitingLayer;

    // informs how many threads are executing operations in each group
    private int[] activeGroup;
    // informs how many threads are executing rotate() on each layer
    private int[][] activeLayer;

    // give permits for executing operations
    private final Semaphore printer = new Semaphore(0, true);
    private Semaphore[][] layerRotate;

    private final Semaphore mutex = new Semaphore(1, true);

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

        waitingGroup = new int[4];
        activeGroup = new int[4];
        for (int i = 0; i < 4; i++) {
            waitingGroup[i] = 0;
            activeGroup[i] = 0;
        }
        waitingLayer = new int[3][size];
        activeLayer = new int[3][size];
        layerRotate = new Semaphore[3][size];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < size; j++) {
                waitingLayer[i][j] = 0;
                activeLayer[i][j] = 0;
                layerRotate[i][j] = new Semaphore(0, true);
            }
        }
    }

    public int getSize() {
        return size;
    }

    public int oppositeSide(int side) {
        switch (side) {
            case 0:
                return 5;
            case 5:
                return 0;
            default:
                return (side + 1) % 4 + 1;
        }
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
        for (int side = 1; side < 4; side++) {
            rewriteRow(side + 1, side, layer, layer);
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
        rewriteColumnToRow(1, 5, layer, size - 1 - layer);
        rewriteRowToColumnConversely(0, 1, layer, layer);
        squares[0][layer] = columnTemp;
    }

    // rotates layer looking from side 5
    // (without squares on side 0 and side 5)
    private void rotatePerimeterLayer5(int layer) {
        int[] rowTemp = copyRow(4, size - 1 - layer);
        for (int side = 4; side > 1; side--) {
            rewriteRow(side - 1, side, size - 1 - layer, size - 1 - layer);
        }
        squares[1][size - 1 - layer] = rowTemp;
    }

    private void rotatePerimeterLayer(int side, int layer) {
        switch (side) {
            case 0:
                rotatePerimeterLayer0(layer);
                break;
            case 1:
                rotatePerimeterLayer1(layer);
                break;
            case 2:
                rotatePerimeterLayer2(layer);
                break;
            case 3:
                rotatePerimeterLayer3(layer);
                break;
            case 4:
                rotatePerimeterLayer4(layer);
                break;
            case 5:
                rotatePerimeterLayer5(layer);
        }
    }

    private void executeRotation(int side, int layer)  {
        if (layer == 0) {
            rightRotateSide(side);
        }
        else if (layer == size - 1) {
            leftRotateSide(oppositeSide(side));
        }

        rotatePerimeterLayer(side, layer);
    }

    private void releaseGroup(int group) {
        if (group < 3) {
            int i = 0;
            while (i < size && waitingLayer[group][i] == 0)
                i++;
            layerRotate[group][i].release();
        }
        else {
            printer.release();
        }
    }

    // waits till rotating is possible, then rotates the cube
    public void rotate(int side, int layer) throws InterruptedException {
        int uniqueSide = side < 3 ? side : oppositeSide(side);
        int uniqueLayer = side < 3 ? layer : size - 1 - layer;

        mutex.acquire();
        if (waitingGroup[(uniqueSide + 1) % 4] > 0 || waitingGroup[(uniqueSide + 2) % 4] > 0 ||
                waitingGroup[(uniqueSide + 3) % 4] > 0 || activeGroup[(uniqueSide + 1) % 4] > 0 ||
                activeGroup[(uniqueSide + 2) % 4] > 0 || activeGroup[(uniqueSide + 3) % 4] > 0 ||
                activeLayer[uniqueSide][uniqueLayer] > 0) {
            waitingGroup[uniqueSide]++;
            waitingLayer[uniqueSide][uniqueLayer]++;
            mutex.release();
            layerRotate[uniqueSide][uniqueLayer].acquire();
            waitingGroup[uniqueSide]--;
            waitingLayer[uniqueSide][uniqueLayer]--;
        }

        activeLayer[uniqueSide][uniqueLayer]++;
        activeGroup[uniqueSide]++;
        int i = 0;
        while (i < size && (activeLayer[uniqueSide][i] > 0 || waitingLayer[uniqueSide][i] == 0))
            i++;
        if (i != size)
            layerRotate[uniqueSide][i].release();
        else
            mutex.release();

        beforeRotation.accept(side, layer);
        executeRotation(side, layer);
        afterRotation.accept(side, layer);

        mutex.acquire();
        activeGroup[uniqueSide]--;
        activeLayer[uniqueSide][uniqueLayer]--;

        if (activeGroup[uniqueSide] == 0) {
            if (waitingGroup[(uniqueSide + 1) % 4] > 0)
                releaseGroup((uniqueSide + 1) % 4);
            else if (waitingGroup[(uniqueSide + 2) % 4] > 0)
                releaseGroup((uniqueSide + 2) % 4);
            else if (waitingGroup[(uniqueSide + 3) % 4] > 0)
                releaseGroup((uniqueSide + 3) % 4);
            else if (waitingGroup[uniqueSide] > 0)
                releaseGroup(uniqueSide);
            else
                mutex.release();
        }
        else {
            mutex.release();
        }
    }

    private String executeShowing() {
        StringBuffer sb = new StringBuffer();
        for (int side = 0; side < 6; side++) {
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    sb.append(squares[side][row][column]);
                }
            }
        }
        return sb.toString();
    }

    // waits till showing is possible, then shows the cube
    public String show() throws InterruptedException {
        mutex.acquire();
        if (activeGroup[0] > 0 || activeGroup[1] > 0 || activeGroup[2] > 0 || activeGroup[3] > 0) {
            waitingGroup[3]++;
            mutex.release();
            printer.acquire();
            waitingGroup[3]--;
        }
        activeGroup[3]++;
        mutex.release();

        beforeShowing.run();
        String result = executeShowing();
        afterShowing.run();

        mutex.acquire();
        activeGroup[3]--;

        int i = 0;
        if (waitingGroup[0] > 0) {
            while (i < size && waitingLayer[0][i] == 0)
                i++;
            layerRotate[0][i].release();
        }
        else if (waitingGroup[1] > 0) {
            while (i < size && waitingLayer[1][i] == 0)
                i++;
            layerRotate[1][i].release();
        }
        else if (waitingGroup[2] > 0) {
            while (i < size && waitingLayer[2][i] == 0)
                i++;
            layerRotate[2][i].release();
        }
        else if (waitingGroup[3] > 0) {
            releaseGroup(3);
        }
        else {
            mutex.release();
        }
        return result;
    }
}
