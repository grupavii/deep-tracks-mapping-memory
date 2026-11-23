package com.grupavii.dtmappingindia.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;

public final class BufferCleaner {

    private BufferCleaner() {}

    private static final Unsafe UNSAFE;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void unmap(MappedByteBuffer buffer) {
        try {
            UNSAFE.invokeCleaner(buffer);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to unmap buffer", e);
        }
    }
}
