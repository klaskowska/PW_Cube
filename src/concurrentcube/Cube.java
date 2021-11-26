package concurrentcube;

import java.util.ArrayList;
import java.util.Random;
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

    // inform how many threads are waiting for operations:
    // show() and rotate() on axis 0, 1 or 2;
    // axis_i crosses side i
    private int waitingPrinter = 0;
    private int waitingAxis0 = 0;
    private int waitingAxis1 = 0;
    private int waitingAxis2 = 0;

    // inform how many threads are executing operations
    private int activePrinter = 0;
    private int activeAxis0 = 0;
    private int activeAxis1 = 0;
    private int activeAxis2 = 0;

    // give permits for executing operations
    private final Semaphore printer = new Semaphore(0, true);
    private final Semaphore axis0 = new Semaphore(0, true);
    private final Semaphore axis1 = new Semaphore(0, true);
    private final Semaphore axis2 = new Semaphore(0, true);

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
    }

    public int getSize() {
        return size;
    }

    private int oppositeSide(int side) {
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

    public void executeRotation(int side, int layer)  {
        if (layer == 0) {
            rightRotateSide(side);
        }
        else if (layer == size - 1) {
            leftRotateSide(oppositeSide(side));
        }

        rotatePerimeterLayer(side, layer);
    }

    // pomysl potem o zrobieniu tablicy na waitingi i na active'y zeby rotateAxis napisac tylko raz
    public void rotateAxis0(int side, int layer) throws InterruptedException {
        mutex.acquire();
        if (waitingAxis1 + waitingAxis2 + waitingPrinter + activeAxis1 + activeAxis2 + activePrinter > 0) {
            waitingAxis0++;
            mutex.release();
            axis0.acquire();
            waitingAxis0--;
        }
        activeAxis0++;
        if (waitingAxis0 > 0) {
            axis0.release();
        }
        else {
            mutex.release();
        }
        beforeRotation.accept(side, layer);
        executeRotation(side, layer);
        afterRotation.accept(side, layer);
        mutex.acquire();
        activeAxis0--;
        if (activeAxis0 == 0) {
            if (waitingAxis1 > 0) {
                axis0.release();
            }
            else if (waitingAxis2 > 0) {
                axis1.release();
            }
            else if (waitingPrinter > 0) {
                printer.release();
            }
            else {
                mutex.release();
            }
        }
        else {
            mutex.release();
        }
    }

    public void rotateAxis1(int side, int layer) throws InterruptedException {
        mutex.acquire();
        if (waitingAxis0 + waitingAxis2 + waitingPrinter + activeAxis0 + activeAxis2 + activePrinter > 0) {
            waitingAxis1++;
            mutex.release();
            axis1.acquire();
            waitingAxis1--;
        }
        activeAxis1++;
        if (waitingAxis1 > 0) {
            axis1.release();
        }
        else {
            mutex.release();
        }
        beforeRotation.accept(side, layer);
        executeRotation(side, layer);
        afterRotation.accept(side, layer);
        mutex.acquire();
        activeAxis1--;
        if (activeAxis1 == 0) {
            if (waitingAxis2 > 0) {
                axis2.release();
            }
            else if (waitingPrinter > 0) {
                printer.release();
            }
            else if (waitingAxis0 > 0) {
                axis0.release();
            }
            else {
                mutex.release();
            }
        }
        else {
            mutex.release();
        }
    }

    public void rotateAxis2(int side, int layer) throws InterruptedException {
        mutex.acquire();
        if (waitingAxis1 + waitingAxis0 + waitingPrinter + activeAxis1 + activeAxis0 + activePrinter > 0) {
            waitingAxis2++;
            mutex.release();
            axis2.acquire();
            waitingAxis2--;
        }
        activeAxis2++;
        if (waitingAxis2 > 0) {
            axis2.release();
        }
        else {
            mutex.release();
        }
        beforeRotation.accept(side, layer);
        executeRotation(side, layer);
        afterRotation.accept(side, layer);
        mutex.acquire();
        activeAxis2--;
        if (activeAxis2 == 0) {
            if (waitingPrinter > 0) {
                printer.release();
            }
            else if (waitingAxis0 > 0) {
                axis0.release();
            }
            else if (waitingAxis1 > 0) {
                axis1.release();
            }
            else {
                mutex.release();
            }
        }
        else {
            mutex.release();
        }
    }

    // waits till rotating is possible, then rotates the cube
    public void rotate(int side, int layer) throws InterruptedException {
        System.out.println("siem");
        if (side == 0 || side == 5)
            rotateAxis0(side, layer);
        else if (side == 1 || side == 3)
            rotateAxis1(side, layer);
        else
            rotateAxis2(side, layer);

    }

    public String executeShowing() {
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
        if (activeAxis0 + activeAxis1 + activeAxis2 + activePrinter > 0) {
            waitingPrinter++;
            mutex.release();
            printer.acquire();
            waitingPrinter--;
        }
        activePrinter++;
        mutex.release();
        beforeShowing.run();
        String result = executeShowing();
        afterShowing.run();
        mutex.acquire();
        activePrinter--;
        if (waitingAxis0 > 0) {
            axis0.release();
        }
        else if (waitingAxis1 > 0) {
            axis1.release();
        }
        else if (waitingAxis2 > 0) {
            axis2.release();
        }
        else {
            mutex.release();
        }
        return result;
    }

    private static class ExecutorRotate implements Runnable {
        private Cube cube;
        private int side;
        private int layer;
        public ExecutorRotate(Cube cube, int side, int layer) {
            this.cube = cube;
            this.side = side;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                cube.rotate(side, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ExecutorShow implements Runnable {
        private Cube cube;
        private ObjectString result;

        public ExecutorShow(Cube cube, ObjectString result) {
            this.cube = cube;
        }
        @Override
        public void run() {
            try {
                System.out.println("hello");
                String st = cube.show();
                //result.setS(st);
                System.out.println(st);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public String getResult() {
            return result.getS();
        }
    }

    private static class ObjectString {
        private String s;

        public void setS(String s) {
            this.s = s;
        }

        public String getS() {
            return s;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var counter = new Object() { int value = 0; };
        int size = 4;
        Cube cube = new Cube(size,
                (x, y) -> { ++counter.value; },
                (x, y) -> { ++counter.value; },
                () -> { ++counter.value; },
                () -> { ++counter.value; }
        );

        Cube cubePerfect = new Cube(size,
                (x, y) -> { ++counter.value; },
                (x, y) -> { ++counter.value; },
                () -> { ++counter.value; },
                () -> { ++counter.value; }
        );

        ArrayList<Thread> threadList = new ArrayList<>();
        ArrayList<Thread> threadList1 = new ArrayList<>();
        ArrayList<ObjectString> resultList = new ArrayList<>();
        int trials = 10;
        int[] randomSide = new int[trials];
        int[] randomLayer = new int[trials];
        Random r = new Random();
        int side;
        for (int i = 0; i < trials; i++) {
            randomSide[i] = r.nextInt(2);
            randomLayer[i] = r.nextInt(cube.size);
            //System.out.println("rotate(" + randomSide[i] + ", " + randomLayer[i] + ")\n");
            if (randomLayer[i] == 0)
                side = 0;
            else
                side = 5;
            Thread t = new Thread(new ExecutorRotate(cube, side, randomLayer[i]));
            t.start();
            threadList.add(t);

            ObjectString result = new ObjectString();
            Runnable runnable = new ExecutorShow(cube, result);
            resultList.add(result);
            Thread t1 = new Thread(runnable);
            t1.start();
            threadList.add(t1);
        }
        for (int i = trials - 1; i >= 0; i--) {
            if (randomLayer[i] == 0)
                side = 0;
            else
                side = 5;
            Thread t = new Thread(new ExecutorRotate(cube, cube.oppositeSide(side), cube.size - 1 - randomLayer[i]));
            t.start();
            threadList.add(t);
            //System.out.println("rotate(" + cube.oppositeSide(randomSide[i]) + ", " + (cube.size - 1 - randomLayer[i]) + ")\n");
        }

        for (Thread t : threadList) {
            t.join();
        }
        for (ObjectString o : resultList) {
            System.out.println(o.getS());
        }

        String cubeString = cube.show();
        String cubePerfectString = cubePerfect.show();

        if (cubeString.equals(cubePerfectString)) {
            System.out.println("ok");
        }

    }
}
