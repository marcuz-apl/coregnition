package io.github.marcuzapl.coregnition.desktop;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import javafx.application.Application;

/** Plain main entry point allows JavaFX to run from the packaged classpath. */
public final class DesktopLauncher {
    public static void main(String[] args) throws Exception {
        var options = new ArrayList<>(Arrays.asList(args));
        Path application = Path.of(DesktopLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        Path bundledBackend = application.resolve("backend.jar");
        if (Files.isRegularFile(bundledBackend) && options.stream().noneMatch(s -> s.startsWith("--backend-jar="))) {
            options.add("--backend-jar=" + bundledBackend);
            if (options.stream().noneMatch(s -> s.startsWith("--data-dir=")))
                options.add("--data-dir=" + PlatformPaths.dataDirectory());
        }
        if (options.stream().anyMatch(s -> s.startsWith("--backend-jar=")) && options.stream().noneMatch(s -> s.startsWith("--port="))) {
            try (ServerSocket socket = new ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
                options.add("--port=" + socket.getLocalPort());
            }
        }
        Application.launch(CoregnitionDesktopApplication.class, options.toArray(String[]::new));
    }
}
