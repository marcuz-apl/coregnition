package io.github.marcuzapl.coregnition.desktop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BackendProcess implements AutoCloseable {
    private Process process;

    void start(Path backendJar, int port) throws IOException {
        if (!Files.isRegularFile(backendJar)) throw new IOException("Backend JAR was not found: " + backendJar);
        process = new ProcessBuilder(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar", backendJar.toString(), "--server.port=" + port)).inheritIO().start();
    }

    @Override public void close() {
        if (process != null && process.isAlive()) process.destroy();
    }
}
