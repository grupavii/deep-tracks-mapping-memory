package com.grupavii.dtmappingindia.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GridDemoRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void preservesExistingMappedGridFiles() throws Exception {
        Path basePath = tempDir.resolve("grid.dat");
        byte[] original = {1, 2, 3, 4};
        for (String suffix : new String[]{".single", ".region", ".panama"}) {
            Files.write(Path.of(basePath + suffix), original);
        }

        GridDemoRunner runner = runner("all", basePath);
        runner.run();

        for (String suffix : new String[]{".single", ".region", ".panama"}) {
            assertArrayEquals(original, Files.readAllBytes(Path.of(basePath + suffix)));
        }
    }

    @Test
    void rejectsUnknownDemoStep() {
        GridDemoRunner runner = runner("typo", tempDir.resolve("grid.dat"));
        assertThrows(IllegalArgumentException.class, runner::run);
    }

    @Test
    void rejectsInvalidRegionConfigurationAndRemovesItsTemporaryFile() {
        GridDemoRunner runner = runner("region-mapped", tempDir.resolve("grid.dat"));
        setField(runner, "regionBytes", 7L);

        assertThrows(IllegalArgumentException.class, runner::run);
        try (var files = Files.list(tempDir)) {
            assertEquals(0, files.count());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private GridDemoRunner runner(String step, Path gridFile) {
        GridDemoRunner runner = new GridDemoRunner();
        setField(runner, "rows", 2);
        setField(runner, "cols", 2);
        setField(runner, "gridFile", gridFile.toString());
        setField(runner, "regionBytes", 1L << 30);
        setField(runner, "step", step);
        return runner;
    }

    private void setField(GridDemoRunner runner, String name, Object value) {
        try {
            var field = GridDemoRunner.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(runner, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}