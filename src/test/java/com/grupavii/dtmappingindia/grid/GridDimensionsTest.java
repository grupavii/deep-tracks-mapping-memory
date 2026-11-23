package com.grupavii.dtmappingindia.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GridDimensionsTest {

    @Test
    void rejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class, () -> GridDimensions.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> GridDimensions.of(1, -1));
    }

    @Test
    void rejectsDimensionsWhoseByteSizeOverflowsLong() {
        assertThrows(IllegalArgumentException.class,
                () -> GridDimensions.of(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void computesCellOffsetsWithinValidatedBounds() {
        GridDimensions dimensions = GridDimensions.of(2, 3);
        assertEquals(40L, dimensions.cellOffset(1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dimensions.cellOffset(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dimensions.cellOffset(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dimensions.cellOffset(0, 3));
    }
}