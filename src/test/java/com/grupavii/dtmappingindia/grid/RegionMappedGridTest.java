package com.grupavii.dtmappingindia.grid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionMappedGridTest {

    @TempDir
    Path tempDir;

    @Test
    void writeAndReadSingleCell() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             RegionMappedGrid grid = new RegionMappedGrid(10, 10, ch)) {
            grid.writeCell(3, 7, 42L);
            assertEquals(42L, grid.readCell(3, 7));
        }
    }

    @Test
    void freshGridReadsAsZero() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             RegionMappedGrid grid = new RegionMappedGrid(10, 10, ch)) {
            assertEquals(0L, grid.readCell(0, 0));
            assertEquals(0L, grid.readCell(9, 9));
            assertEquals(0L, grid.readCell(5, 5));
        }
    }

    @Test
    void writeAndReadAcrossRegionBoundary() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        // 64 bytes per region = 8 longs per region
        long smallRegion = 64;
        int rows = 4;
        int cols = 4; // 16 cells total = 128 bytes → 2 regions

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             RegionMappedGrid grid = new RegionMappedGrid(rows, cols, ch, smallRegion)) {

            assertEquals(2, grid.regionCount());

            // Write in first region (linear index 0-7)
            grid.writeCell(0, 0, 111L);
            grid.writeCell(1, 3, 222L); // linear index 7 = last cell in region 0

            // Write in second region (linear index 8-15)
            grid.writeCell(2, 0, 333L); // linear index 8 = first cell in region 1
            grid.writeCell(3, 3, 444L); // linear index 15 = last cell in region 1

            assertEquals(111L, grid.readCell(0, 0));
            assertEquals(222L, grid.readCell(1, 3));
            assertEquals(333L, grid.readCell(2, 0));
            assertEquals(444L, grid.readCell(3, 3));
        }
    }

    @Test
    void boundaryConditions() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        int rows = 100;
        int cols = 100;

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             RegionMappedGrid grid = new RegionMappedGrid(rows, cols, ch)) {

            // First cell
            grid.writeCell(0, 0, 1L);
            assertEquals(1L, grid.readCell(0, 0));

            // Last cell
            grid.writeCell(rows - 1, cols - 1, 2L);
            assertEquals(2L, grid.readCell(rows - 1, cols - 1));

            // Middle cell
            grid.writeCell(rows / 2, cols / 2, 3L);
            assertEquals(3L, grid.readCell(rows / 2, cols / 2));
        }
    }

    @Test
    void regionBoundaryCells() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        // 80 bytes per region = 10 longs per region
        long smallRegion = 80;
        int rows = 5;
        int cols = 4; // 20 cells = 160 bytes → 2 regions

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             RegionMappedGrid grid = new RegionMappedGrid(rows, cols, ch, smallRegion)) {

            assertEquals(2, grid.regionCount());

            // Last cell of first region (linear index 9)
            grid.writeCell(2, 1, 999L);
            assertEquals(999L, grid.readCell(2, 1));

            // First cell of second region (linear index 10)
            grid.writeCell(2, 2, 888L);
            assertEquals(888L, grid.readCell(2, 2));
        }
    }

    @Test
    void rejectsColumnOutsideGridInsteadOfAliasingNextRow() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             RegionMappedGrid grid = new RegionMappedGrid(2, 2, ch)) {
            assertThrows(IndexOutOfBoundsException.class, () -> grid.readCell(0, 2));
        }
    }

    @Test
    void rejectsRegionSizesThatCannotHoldWholeCells() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            assertThrows(IllegalArgumentException.class,
                    () -> new RegionMappedGrid(2, 2, ch, Long.BYTES - 1));
        }
    }

    @Test
    void rejectsAccessAfterCloseAndAllowsRepeatedClose() throws Exception {
        Path file = tempDir.resolve("grid.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            RegionMappedGrid grid = new RegionMappedGrid(2, 2, ch);
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
            try (RegionMappedGrid grid = new RegionMappedGrid(2, 2, ch, 16)) {
                grid.writeCell(1, 1, 42L);
            }
            try (RegionMappedGrid grid = new RegionMappedGrid(2, 2, ch, 16)) {
                assertEquals(42L, grid.readCell(1, 1));
            }
        }
    }
}
