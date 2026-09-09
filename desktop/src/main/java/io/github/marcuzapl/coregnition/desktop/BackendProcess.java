package io.github.marcuzapl.coregnition.desktop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BackendProcess implements AutoCloseable {
    private Process process;
    private boolean closed;

    synchronized void start(Path backendJar, int port, Path dataDirectory) throws IOException {
        if (closed) throw new IOException("Desktop has already closed");
        if (!Files.isRegularFile(backendJar)) throw new IOException("Backend JAR was not found: " + backendJar);
        Files.createDirectories(dataDirectory);
        process = new ProcessBuilder(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar", backendJar.toString(), "--server.port=" + port, "--coregnition.database=" + dataDirectory.resolve("coregnition.db").toAbsolutePath(), "--coregnition.storage-root=" + dataDirectory.resolve("projects").toAbsolutePath())).inheritIO().start();
    }

    @Override public synchronized void close() {
        closed = true;
        if (process != null && process.isAlive()) {
            process.destroy();
            try { if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly(); }
            catch (InterruptedException e) { process.destroyForcibly(); Thread.currentThread().interrupt(); }
        }
    }
}
