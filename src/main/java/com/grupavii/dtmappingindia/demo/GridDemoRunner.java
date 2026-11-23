package com.grupavii.dtmappingindia.demo;

import com.grupavii.dtmappingindia.grid.NaiveHeapGrid;
import com.grupavii.dtmappingindia.grid.PanamaGrid;
import com.grupavii.dtmappingindia.grid.RegionMappedGrid;
import com.grupavii.dtmappingindia.grid.SingleBufferGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class GridDemoRunner implements CommandLineRunner {

    @Value("${grid.rows:1000}")
    private int rows;

    @Value("${grid.cols:1000}")
    private int cols;

    @Value("${grid.file:grid.dat}")
    private String gridFile;

    @Value("${grid.region-bytes:1073741824}")
    private long regionBytes;

    @Value("${demo.step:all}")
    private String step;

    @Override
    public void run(String... args) throws Exception {
        long bytes = gridBytes();
        System.out.printf("%nGrid dimensions: %,d × %,d (%,d cells; %,d bytes)%n%n",
                rows, cols, (long) rows * cols, bytes);

        switch (step) {
            case "naive" -> demoNaiveHeap();
            case "single-buffer" -> demoSingleBuffer();
            case "region-mapped" -> demoRegionMapped();
            case "panama" -> demoPanama();
            case "all" -> {
                demoNaiveHeap();
                System.out.println();
                demoSingleBuffer();
                System.out.println();
                demoRegionMapped();
                System.out.println();
                demoPanama();
            }
            default -> throw new IllegalArgumentException("Unknown demo.step: " + step);
        }
    }

    private void demoNaiveHeap() {
        System.out.println("=== Step 1: Naive Heap Grid (on-heap) ===");
        System.out.printf("  -Xmx: %,d bytes (%d MB)%n",
                Runtime.getRuntime().maxMemory(),
                Runtime.getRuntime().maxMemory() / (1024 * 1024));

        try {
            try (NaiveHeapGrid grid = new NaiveHeapGrid(rows, cols)) {
                grid.writeCell(0, 0, 42L);
                grid.writeCell(rows - 1, cols - 1, 99L);
                grid.writeCell(rows / 2, cols / 2, 7L);

                System.out.printf("  Cell [0,0]       = %d%n", grid.readCell(0, 0));
                System.out.printf("  Cell [%d,%d] = %d%n",
                        rows - 1, cols - 1, grid.readCell(rows - 1, cols - 1));
                System.out.printf("  Cell [%d,%d]   = %d%n",
                        rows / 2, cols / 2, grid.readCell(rows / 2, cols / 2));
                System.out.println("  " + heapStatus());
            }
        } catch (OutOfMemoryError e) {
            System.out.println("  *** OutOfMemoryError — this is exactly the failure from the blog post. ***");
            System.out.printf("  The primitive grid needs at least %,d MB but -Xmx is only %d MB.%n",
                    gridBytes() / (1024 * 1024),
                    Runtime.getRuntime().maxMemory() / (1024 * 1024));
            System.out.println("  " + heapStatus());
        }
    }

    private void demoSingleBuffer() throws Exception {
        System.out.println("=== Step 2: Single MappedByteBuffer Grid (off-heap) ===");
        if (gridBytes() > Integer.MAX_VALUE) {
            System.out.println("  Expected limit: a single MappedByteBuffer cannot map more than "
                    + Integer.MAX_VALUE + " bytes.");
            System.out.printf("  This grid needs %,d bytes; use RegionMappedGrid or PanamaGrid instead.%n",
                    gridBytes());
            return;
        }

        Path path = createTemporaryGridFile("single");
        try {
            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
                 FileChannel channel = raf.getChannel();
                 SingleBufferGrid grid = new SingleBufferGrid(rows, cols, channel)) {

                grid.writeCell(0, 0, 42L);
                grid.writeCell(rows - 1, cols - 1, 99L);
                grid.writeCell(rows / 2, cols / 2, 7L);

                System.out.printf("  Cell [0,0]       = %d%n", grid.readCell(0, 0));
                System.out.printf("  Cell [%d,%d] = %d%n",
                        rows - 1, cols - 1, grid.readCell(rows - 1, cols - 1));
                System.out.printf("  Cell [%d,%d]   = %d%n",
                        rows / 2, cols / 2, grid.readCell(rows / 2, cols / 2));
                System.out.println("  " + heapStatus());
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private void demoRegionMapped() throws Exception {
        System.out.println("=== Step 3: Region-Mapped Grid (the solution) ===");
        Path path = createTemporaryGridFile("region");

        try {
            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
                 FileChannel channel = raf.getChannel();
                 RegionMappedGrid grid = new RegionMappedGrid(rows, cols, channel, regionBytes)) {

                System.out.printf("  Mapped %d region(s), file size: %,d bytes%n",
                        grid.regionCount(), channel.size());

                grid.writeCell(0, 0, 42L);
                grid.writeCell(rows - 1, cols - 1, 99L);
                grid.writeCell(rows / 2, cols / 2, 7L);

                System.out.printf("  Cell [0,0]       = %d%n", grid.readCell(0, 0));
                System.out.printf("  Cell [%d,%d] = %d%n",
                        rows - 1, cols - 1, grid.readCell(rows - 1, cols - 1));
                System.out.printf("  Cell [%d,%d]   = %d%n",
                        rows / 2, cols / 2, grid.readCell(rows / 2, cols / 2));
                System.out.println("  " + heapStatus());
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private void demoPanama() throws Exception {
        System.out.println("=== Step 4: Panama Grid (java.lang.foreign) ===");
        Path path = createTemporaryGridFile("panama");

        try {
            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
                 FileChannel channel = raf.getChannel();
                 PanamaGrid grid = new PanamaGrid(rows, cols, channel)) {

                System.out.printf("  Single MemorySegment, file size: %,d bytes%n",
                        channel.size());

                grid.writeCell(0, 0, 42L);
                grid.writeCell(rows - 1, cols - 1, 99L);
                grid.writeCell(rows / 2, cols / 2, 7L);

                System.out.printf("  Cell [0,0]       = %d%n", grid.readCell(0, 0));
                System.out.printf("  Cell [%d,%d] = %d%n",
                        rows - 1, cols - 1, grid.readCell(rows - 1, cols - 1));
                System.out.printf("  Cell [%d,%d]   = %d%n",
                        rows / 2, cols / 2, grid.readCell(rows / 2, cols / 2));
                System.out.println("  No --add-opens required. No Unsafe. No region splitting.");
                System.out.println("  " + heapStatus());
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private long gridBytes() {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Grid rows and columns must be positive");
        }
        try {
            return Math.multiplyExact(Math.multiplyExact((long) rows, cols), Long.BYTES);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Grid dimensions exceed the supported byte size", e);
        }
    }

    private Path createTemporaryGridFile(String suffix) throws IOException {
        Path configuredPath = Path.of(gridFile).toAbsolutePath().normalize();
        Path parent = configuredPath.getParent();
        String prefix = configuredPath.getFileName() + "." + suffix + ".";
        return Files.createTempFile(parent, prefix, ".dat");
    }

    private String heapStatus() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        return String.format("Heap: %,d MB used / %,d MB max (%.1f%%)",
                used / (1024 * 1024), max / (1024 * 1024),
                100.0 * used / max);
    }
}
