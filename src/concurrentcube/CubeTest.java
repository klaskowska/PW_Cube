package concurrentcube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CubeTest {

    // rotates and then rotates conversely using one thread
    // checks if result is like at the beginning
    @Test
    public void testSequentially() throws InterruptedException {
        var counter = new Object() { int value = 0; };
        int size = 100;
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

        int trials = 100000;
        int[] randomSide = new int[trials];
        int[] randomLayer = new int[trials];
        Random r = new Random();
        for (int i = 0; i < trials; i++) {
            randomSide[i] = r.nextInt(6);
            randomLayer[i] = r.nextInt(cube.getSize());
            //System.out.println("rotate(" + randomSide[i] + ", " + randomLayer[i] + ")\n");
            cube.rotate(randomSide[i], randomLayer[i]);
        }
        for (int i = trials - 1; i >= 0; i--) {
            //System.out.println("rotate(" + cube.oppositeSide(randomSide[i]) + ", " + (cube.size - 1 - randomLayer[i]) + ")\n");
            cube.rotate(cube.oppositeSide(randomSide[i]), cube.getSize() - 1 - randomLayer[i]);
        }
        String cubeString = cube.show();
        String cubePerfectString = cubePerfect.show();
        assertEquals(cubeString, cubePerfectString);
    }

    // rotates only on axis0 and then rotates conversely (also executes show())
    // using more than one thread
    // checks if result is like at the beginning
    @Test
    public void testConcurrentAxis0() throws InterruptedException {
        int trials = 100;
        int size = 40;
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var counter = new Object() { int value = 0; };

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

        int[] randomSide = new int[trials];
        int[] randomLayer = new int[trials];
        Random r = new Random();
        int side;
        for (int i = 0; i < trials; i++) {
            randomSide[i] = r.nextInt(2);
            randomLayer[i] = r.nextInt(cube.getSize());
            if (randomLayer[i] == 0)
                side = 0;
            else
                side = 5;
            int finalSide = side;
            int finalI = i;
            executor.execute(() -> {
                try {
                    cube.rotate(finalSide, randomLayer[finalI]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            executor.execute(() -> {
                try {
                    cube.show();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        for (int i = trials - 1; i >= 0; i--) {
            if (randomLayer[i] == 0)
                side = 0;
            else
                side = 5;
            int finalSide1 = side;
            int finalI = i;
            executor.execute(() -> {
                try {
                    cube.rotate(cube.oppositeSide(finalSide1), cube.getSize() - 1 - randomLayer[finalI]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();

        executor.awaitTermination(20, TimeUnit.SECONDS);

        String cubeString = cube.show();
        String cubePerfectString = cubePerfect.show();
        assertEquals(cubeString, cubePerfectString);
    }

    // rotates only on axis1 and then rotates conversely (also executes show())
    // using more than one thread
    // checks if result is like at the beginning
    @Test
    public void testConcurrentAxis1() throws InterruptedException {
        int trials = 100;
        int size = 40;
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var counter = new Object() { int value = 0; };

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

        int[] randomSide = new int[trials];
        int[] randomLayer = new int[trials];
        Random r = new Random();
        int side;
        for (int i = 0; i < trials; i++) {
            randomSide[i] = r.nextInt(2);
            randomLayer[i] = r.nextInt(cube.getSize());
            if (randomLayer[i] == 0)
                side = 1;
            else
                side = 3;
            int finalSide = side;
            int finalI = i;
            executor.execute(() -> {
                try {
                    cube.rotate(finalSide, randomLayer[finalI]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            executor.execute(() -> {
                try {
                    cube.show();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        for (int i = trials - 1; i >= 0; i--) {
            if (randomLayer[i] == 0)
                side = 1;
            else
                side = 3;
            int finalSide1 = side;
            int finalI = i;
            executor.execute(() -> {
                try {
                    cube.rotate(cube.oppositeSide(finalSide1), cube.getSize() - 1 - randomLayer[finalI]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();

        executor.awaitTermination(20, TimeUnit.SECONDS);

        String cubeString = cube.show();
        String cubePerfectString = cubePerfect.show();
        assertEquals(cubeString, cubePerfectString);
    }

    // rotates only on axis2 and then rotates conversely (also executes show())
    // using more than one thread
    // checks if result is like at the beginning
    @Test
    public void testConcurrentAxis2() throws InterruptedException {
        int trials = 100;
        int size = 40;
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var counter = new Object() { int value = 0; };

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

        int[] randomSide = new int[trials];
        int[] randomLayer = new int[trials];
        Random r = new Random();
        int side;
        for (int i = 0; i < trials; i++) {
            randomSide[i] = r.nextInt(2);
            randomLayer[i] = r.nextInt(cube.getSize());
            if (randomLayer[i] == 0)
                side = 2;
            else
                side = 4;
            int finalSide = side;
            int finalI = i;
            executor.execute(() -> {
                try {
                    cube.rotate(finalSide, randomLayer[finalI]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            executor.execute(() -> {
                try {
                    cube.show();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        for (int i = trials - 1; i >= 0; i--) {
            if (randomLayer[i] == 0)
                side = 2;
            else
                side = 4;
            int finalSide1 = side;
            int finalI = i;
            executor.execute(() -> {
                try {
                    cube.rotate(cube.oppositeSide(finalSide1), cube.getSize() - 1 - randomLayer[finalI]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();

        executor.awaitTermination(20, TimeUnit.SECONDS);

        String cubeString = cube.show();
        String cubePerfectString = cubePerfect.show();
        assertEquals(cubeString, cubePerfectString);
    }

    // checks if operations on cube are executing concurrently (on axis0):
    // executing time should be less than executing time sequentially
    // (I've picked constants - sequentially it takes about 7.5 sec)
    @Test
    public void testFastnessAxis0() throws InterruptedException {
        long startTime = System.nanoTime();

        int trials = 30000;
        int size = 5000;
        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var counter = new Object() { int value = 0; };

        Cube cube = new Cube(size,
                (x, y) -> { ++counter.value; },
                (x, y) -> { ++counter.value; },
                () -> { ++counter.value; },
                () -> { ++counter.value; }
        );

        for (int i = 0; i < trials; i++) {
            int layer = i % size;
            int side = i % 2 == 0 ? 0 : 5;
            executor.execute(() -> {
                try {
                    cube.rotate(side, layer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        assert(((endTime - startTime) / 1000000) < 7000);
    }

    // checks if operations on cube are executing concurrently (on axis1):
    // executing time should be less than executing time sequentially
    // (I've picked constants - sequentially it takes about 12 sec)
    @Test
    public void testFastnessAxis1() throws InterruptedException {
        long startTime = System.nanoTime();

        int trials = 10000;
        int size = 4000;
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var counter = new Object() { int value = 0; };

        Cube cube = new Cube(size,
                (x, y) -> { ++counter.value; },
                (x, y) -> { ++counter.value; },
                () -> { ++counter.value; },
                () -> { ++counter.value; }
        );

        for (int i = 0; i < trials; i++) {
            int layer = i % size;
            int side = i % 2 == 0 ? 1 : 3;
            executor.execute(() -> {
                try {
                    cube.rotate(side, layer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        assert(((endTime - startTime) / 1000000) < 10000);
    }

    // checks if operations on cube are executing concurrently (on axis2):
    // executing time should be less than executing time sequentially
    // (I've picked constants - sequentially it takes about 9 sec)
    @Test
    public void testFastnessAxis2() throws InterruptedException {
        long startTime = System.nanoTime();

        int trials = 100000;
        int size = 1600;
        int threads = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        var counter = new Object() { int value = 0; };

        Cube cube = new Cube(size,
                (x, y) -> { ++counter.value; },
                (x, y) -> { ++counter.value; },
                () -> { ++counter.value; },
                () -> { ++counter.value; }
        );

        for (int i = 0; i < trials; i++) {
            int layer = i % size;
            int side = i % 2 == 0 ? 2 : 4;
            executor.execute(() -> {
                try {
                    cube.rotate(side, layer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        assert(((endTime - startTime) / 1000000) < 8000);
    }
}
