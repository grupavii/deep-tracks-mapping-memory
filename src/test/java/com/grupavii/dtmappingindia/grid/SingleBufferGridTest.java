package com.grupavii.dtmappingindia.grid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleBufferGridTest {

    @TempDir
    Path tempDir;

    @Test
    void writeAndReadSmallGrid() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             SingleBufferGrid grid = new SingleBufferGrid(10, 10, ch)) {

            grid.writeCell(0, 0, 1L);
            grid.writeCell(9, 9, 2L);
            grid.writeCell(5, 5, 3L);

            assertEquals(1L, grid.readCell(0, 0));
            assertEquals(2L, grid.readCell(9, 9));
            assertEquals(3L, grid.readCell(5, 5));
            assertEquals(0L, grid.readCell(3, 3)); // unwritten cell
        }
    }

    @Test
    void failsForGridExceeding2GB() {
        Path file = tempDir.resolve("grid.dat");
        // 20_000 * 20_000 * 8 bytes = 3.2 GB → exceeds Integer.MAX_VALUE
        assertThrows(IllegalArgumentException.class, () -> {
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
                 FileChannel ch = raf.getChannel();
                 SingleBufferGrid grid = new SingleBufferGrid(20_000, 20_000, ch)) {
                // should not reach here
            }
        });
    }

    @Test
    void rejectsColumnOutsideGridInsteadOfAliasingNextRow() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             SingleBufferGrid grid = new SingleBufferGrid(2, 2, ch)) {
            assertThrows(IndexOutOfBoundsException.class, () -> grid.readCell(0, 2));
        }
    }

    @Test
    void rejectsAccessAfterCloseAndAllowsRepeatedClose() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            SingleBufferGrid grid = new SingleBufferGrid(2, 2, ch);
            grid.close();
            grid.close();
            assertThrows(IllegalStateException.class, () -> grid.readCell(0, 0));
            assertThrows(IllegalStateException.class, () -> grid.writeCell(0, 0, 1L));
        }
    }

    @Test
    void persistsValuesWhenReopened() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            try (SingleBufferGrid grid = new SingleBufferGrid(2, 2, ch)) {
                grid.writeCell(1, 1, 42L);
            }
            try (SingleBufferGrid grid = new SingleBufferGrid(2, 2, ch)) {
                assertEquals(42L, grid.readCell(1, 1));
            }
        }
    }
}
