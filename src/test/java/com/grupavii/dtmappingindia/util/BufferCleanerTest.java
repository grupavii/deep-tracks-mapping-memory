package com.grupavii.dtmappingindia.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BufferCleanerTest {

    @TempDir
    Path tempDir;

    @Test
    void unmapCompletesWithoutError() throws Exception {
        Path file = tempDir.resolve("test.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw");
             FileChannel ch = raf.getChannel()) {
            MappedByteBuffer buffer = ch.map(FileChannel.MapMode.READ_WRITE, 0, 1024);
            assertDoesNotThrow(() -> BufferCleaner.unmap(buffer));
        }
    }
}
