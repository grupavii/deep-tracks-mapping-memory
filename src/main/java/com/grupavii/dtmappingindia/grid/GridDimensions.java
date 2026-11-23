package com.grupavii.dtmappingindia.grid;

record GridDimensions(int rows, int cols, long totalBytes) {

    static GridDimensions of(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Grid rows and columns must be positive");
        }

        try {
            long totalBytes = Math.multiplyExact(
                    Math.multiplyExact((long) rows, cols), Long.BYTES);
            return new GridDimensions(rows, cols, totalBytes);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Grid dimensions exceed the supported byte size", e);
        }
    }

    long cellOffset(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException(
                    "Cell (" + row + ", " + col + ") is outside grid " + rows + " × " + cols);
        }
        return ((long) row * cols + col) * Long.BYTES;
    }
}