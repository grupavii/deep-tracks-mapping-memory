package com.grupavii.dtmappingindia.grid;

// Don't do this. Shown for illustration.
public class NaiveHeapGrid implements LongGrid {

    private final long[][] grid;
    private final GridDimensions dimensions;

    public NaiveHeapGrid(int rows, int cols) {
        this.dimensions = GridDimensions.of(rows, cols);
        this.grid = new long[rows][cols];
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
    public long readCell(int row, int col) {
        dimensions.cellOffset(row, col);
        return grid[row][col];
    }

    @Override
    public void writeCell(int row, int col, long value) {
        dimensions.cellOffset(row, col);
        grid[row][col] = value;
    }
}
