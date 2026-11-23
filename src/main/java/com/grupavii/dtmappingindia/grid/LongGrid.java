package com.grupavii.dtmappingindia.grid;

public interface LongGrid extends AutoCloseable {

    int rows();

    int cols();

    long readCell(int row, int col);

    void writeCell(int row, int col, long value);

    @Override
    default void close() {
    }
}