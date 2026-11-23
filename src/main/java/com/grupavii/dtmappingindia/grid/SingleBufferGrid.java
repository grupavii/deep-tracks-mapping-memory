package com.grupavii.dtmappingindia.grid;

import com.grupavii.dtmappingindia.util.BufferCleaner;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class SingleBufferGrid implements LongGrid {

    private final MappedByteBuffer buffer;
    private final GridDimensions dimensions;
    private boolean closed;

    public SingleBufferGrid(int rows, int cols, FileChannel channel) throws IOException {
        this.dimensions = GridDimensions.of(rows, cols);
        Objects.requireNonNull(channel, "channel");
        if (dimensions.totalBytes() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Grid exceeds the MappedByteBuffer limit of "
                    + Integer.MAX_VALUE + " bytes; use RegionMappedGrid or PanamaGrid");
        }
        this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, dimensions.totalBytes());
        buffer.order(ByteOrder.nativeOrder());
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
    public synchronized void writeCell(int row, int col, long value) {
        ensureOpen();
        buffer.putLong((int) dimensions.cellOffset(row, col), value);
    }

    @Override
    public synchronized long readCell(int row, int col) {
        ensureOpen();
        return buffer.getLong((int) dimensions.cellOffset(row, col));
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            BufferCleaner.unmap(buffer);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Grid is closed");
        }
    }
}
