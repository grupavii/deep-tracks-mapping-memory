package com.grupavii.dtmappingindia.grid;

import com.grupavii.dtmappingindia.util.BufferCleaner;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class RegionMappedGrid implements LongGrid {

    private static final long MAX_REGION_BYTES = 1L << 30; // 1 GiB per region

    private final MappedByteBuffer[] regions;
    private final long cellsPerRegion;
    private final GridDimensions dimensions;
    private boolean closed;

    public RegionMappedGrid(int rows, int cols, FileChannel channel) throws IOException {
        this(rows, cols, channel, MAX_REGION_BYTES);
    }

    public RegionMappedGrid(int rows, int cols, FileChannel channel, long maxRegionBytes)
            throws IOException {
        this.dimensions = GridDimensions.of(rows, cols);
        Objects.requireNonNull(channel, "channel");
        if (maxRegionBytes <= 0 || maxRegionBytes > Integer.MAX_VALUE
                || maxRegionBytes % Long.BYTES != 0) {
            throw new IllegalArgumentException("Region size must be a positive, long-aligned value "
                    + "no greater than " + Integer.MAX_VALUE);
        }
        this.cellsPerRegion = maxRegionBytes / Long.BYTES;
        long regionCount = 1 + (dimensions.totalBytes() - 1) / maxRegionBytes;
        if (regionCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Grid requires too many mapped regions: " + regionCount);
        }
        this.regions = new MappedByteBuffer[(int) regionCount];

        try {
            for (int i = 0; i < regions.length; i++) {
                long offset = (long) i * maxRegionBytes;
                long size = Math.min(maxRegionBytes, dimensions.totalBytes() - offset);
                regions[i] = channel.map(FileChannel.MapMode.READ_WRITE, offset, size);
                regions[i].order(ByteOrder.nativeOrder());
            }
        } catch (IOException | RuntimeException | Error failure) {
            unmapRegions(failure);
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
        long linearIndex = dimensions.cellOffset(row, col) / Long.BYTES;
        int regionIndex = (int) (linearIndex / cellsPerRegion);
        int offsetInRegion = (int) (
            (linearIndex % cellsPerRegion) * Long.BYTES
        );
        return regions[regionIndex].getLong(offsetInRegion);
    }

    @Override
    public synchronized void writeCell(int row, int col, long value) {
        ensureOpen();
        long linearIndex = dimensions.cellOffset(row, col) / Long.BYTES;
        int regionIndex = (int) (linearIndex / cellsPerRegion);
        int offsetInRegion = (int) (
            (linearIndex % cellsPerRegion) * Long.BYTES
        );
        regions[regionIndex].putLong(offsetInRegion, value);
    }

    public int regionCount() {
        return regions.length;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            unmapRegions(null);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Grid is closed");
        }
    }

    private void unmapRegions(Throwable originalFailure) {
        Throwable cleanupFailure = null;
        for (MappedByteBuffer region : regions) {
            if (region == null) {
                continue;
            }
            try {
                BufferCleaner.unmap(region);
            } catch (RuntimeException | Error e) {
                if (originalFailure != null) {
                    originalFailure.addSuppressed(e);
                } else if (cleanupFailure == null) {
                    cleanupFailure = e;
                } else {
                    cleanupFailure.addSuppressed(e);
                }
            }
        }
        if (cleanupFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (cleanupFailure instanceof Error error) {
            throw error;
        }
    }
}
