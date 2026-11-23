package com.grupavii.dtmappingindia.grid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PanamaGridTest {

    @TempDir
    Path tempDir;

    @Test
    void writeAndReadSingleCell() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             PanamaGrid grid = new PanamaGrid(10, 10, ch)) {
            grid.writeCell(3, 7, 42L);
            assertEquals(42L, grid.readCell(3, 7));
        }
    }

    @Test
    void freshGridReadsAsZero() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             PanamaGrid grid = new PanamaGrid(10, 10, ch)) {
            assertEquals(0L, grid.readCell(0, 0));
            assertEquals(0L, grid.readCell(9, 9));
            assertEquals(0L, grid.readCell(5, 5));
        }
    }

    @Test
    void boundaryConditions() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        int rows = 100;
        int cols = 100;

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             PanamaGrid grid = new PanamaGrid(rows, cols, ch)) {

            grid.writeCell(0, 0, 1L);
            assertEquals(1L, grid.readCell(0, 0));

            grid.writeCell(rows - 1, cols - 1, 2L);
            assertEquals(2L, grid.readCell(rows - 1, cols - 1));

            grid.writeCell(rows / 2, cols / 2, 3L);
            assertEquals(3L, grid.readCell(rows / 2, cols / 2));
        }
    }

    @Test
    void acceptsGridLargerThan2GB() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        int rows = 20_000;
        int cols = 20_000; // 20000 * 20000 * 8 = 3.2 GB (sparse file)

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             PanamaGrid grid = new PanamaGrid(rows, cols, ch)) {

            grid.writeCell(0, 0, 1L);
            grid.writeCell(rows - 1, cols - 1, 2L);

            assertEquals(1L, grid.readCell(0, 0));
            assertEquals(2L, grid.readCell(rows - 1, cols - 1));
        }
    }

    @Test
    void closingArenaPreventsAccess() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        PanamaGrid grid;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            grid = new PanamaGrid(10, 10, ch);
            grid.writeCell(0, 0, 42L);
            grid.close();
            assertThrows(IllegalStateException.class, () -> grid.readCell(0, 0));
        }
    }

    @Test
    void rejectsColumnOutsideGridInsteadOfAliasingNextRow() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             PanamaGrid grid = new PanamaGrid(2, 2, ch)) {
            assertThrows(IndexOutOfBoundsException.class, () -> grid.readCell(0, 2));
        }
    }

    @Test
    void allowsRepeatedCloseAndRejectsWritesAfterClose() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            PanamaGrid grid = new PanamaGrid(2, 2, ch);
            grid.close();
            grid.close();
            assertThrows(IllegalStateException.class, () -> grid.writeCell(0, 0, 1L));
        }
    }

    @Test
    void persistsValuesWhenReopened() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            try (PanamaGrid grid = new PanamaGrid(2, 2, ch)) {
                grid.writeCell(1, 1, 42L);
            }
            try (PanamaGrid grid = new PanamaGrid(2, 2, ch)) {
                assertEquals(42L, grid.readCell(1, 1));
            }
        }
    }
}
