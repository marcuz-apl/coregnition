package io.github.marcuzapl.coregnition.desktop;

import java.nio.file.Path;
import java.util.Locale;

final class PlatformPaths {
    private PlatformPaths() {}

    static Path dataDirectory() {
        return dataDirectory(System.getProperty("os.name"), System.getProperty("user.home"), System.getenv("LOCALAPPDATA"));
    }

    static Path dataDirectory(String osName, String userHome, String localAppData) {
        if (isWindows(osName)) {
            Path root = localAppData == null || localAppData.isBlank()
                    ? Path.of(userHome, "AppData", "Local") : Path.of(localAppData);
            return root.resolve("Coregnition");
        }
        return Path.of(userHome, ".local", "share", "coregnition");
    }

    static Path javaExecutable() {
        return javaExecutable(System.getProperty("os.name"), System.getProperty("java.home"));
    }

    static Path javaExecutable(String osName, String javaHome) {
        return Path.of(javaHome, "bin", isWindows(osName) ? "java.exe" : "java");
    }

    private static boolean isWindows(String osName) {
        return osName.toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
