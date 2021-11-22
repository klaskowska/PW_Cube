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

    private int[][] copySide(int side) {
        int[][] copy = new int[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++)
                copy[row][column] = squares[side][row][column];
        }
        return copy;
    }

    // rotates only top squares on the side
    // this operation is only for rotate(side, 0)
    private void rotateSide(int side) {
        int colour;
        int[][] copySide =  copySide(side);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++)
                squares[side][row][column] = copySide[column][size - 1 - row];
        }
    }

    // rotates horizontal layer looking from side 0
    // (without squares on side 0 and side 5)
    private void rotateLayerHorizontal0(int layer) {
        int colourTemp;
        for (int column = 0; column < size; column++) {
            colourTemp = squares[1][layer][column];
            for (int i = 0; i < 4; i++) {
                
            }
        }
    }

}
