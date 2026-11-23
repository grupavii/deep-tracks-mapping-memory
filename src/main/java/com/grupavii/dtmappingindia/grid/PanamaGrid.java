package com.grupavii.dtmappingindia.grid;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class PanamaGrid implements LongGrid {

    private static final ValueLayout.OfLong NATIVE_LONG =
            ValueLayout.JAVA_LONG.withOrder(ByteOrder.nativeOrder());

    private final MemorySegment segment;
    private final GridDimensions dimensions;
    private final Arena arena;
    private final boolean ownsArena;
    private boolean closed;

    public PanamaGrid(int rows, int cols, FileChannel channel) throws IOException {
        this(GridDimensions.of(rows, cols), channel, Arena.ofConfined(), true);
    }

    PanamaGrid(int rows, int cols, FileChannel channel, Arena arena) throws IOException {
        this(GridDimensions.of(rows, cols), channel, arena, false);
    }

    private PanamaGrid(GridDimensions dimensions, FileChannel channel, Arena arena, boolean ownsArena)
            throws IOException {
        this.dimensions = dimensions;
        this.arena = Objects.requireNonNull(arena, "arena");
        this.ownsArena = ownsArena;
        try {
            Objects.requireNonNull(channel, "channel");
            this.segment = channel.map(
                    FileChannel.MapMode.READ_WRITE, 0, dimensions.totalBytes(), arena);
        } catch (IOException | RuntimeException | Error failure) {
            closeOwnedArena(failure);
            throw failure;
        }
    }

    @Override
    public int rows() {
        return dimensions.rows();
    }

    @Override
    public int cols() {
        return dimensions.cols();
    }

    @Override
    public synchronized long readCell(int row, int col) {
        ensureOpen();
        return segment.get(NATIVE_LONG, dimensions.cellOffset(row, col));
    }

    @Override
    public synchronized void writeCell(int row, int col, long value) {
        ensureOpen();
        segment.set(NATIVE_LONG, dimensions.cellOffset(row, col), value);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            if (ownsArena) {
                arena.close();
            }
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed || !segment.scope().isAlive()) {
            throw new IllegalStateException("Grid is closed");
        }
    }

    private void closeOwnedArena(Throwable originalFailure) {
        if (ownsArena) {
            try {
                arena.close();
            } catch (RuntimeException | Error cleanupFailure) {
                originalFailure.addSuppressed(cleanupFailure);
            }
        }
    }
}
