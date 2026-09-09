package io.github.marcuzapl.coregnition.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PlatformPathsTest {
    @Test void windowsUsesLocalAppDataIncludingSpaces() {
        assertEquals(Path.of("/profiles/Ada Lovelace/local/Coregnition"),
                PlatformPaths.dataDirectory("Windows 11", "/profiles/Ada Lovelace", "/profiles/Ada Lovelace/local"));
    }
    @Test void windowsFallsBackWhenLocalAppDataIsMissingOrBlank() {
        for (String local : new String[] {null, "", "  "}) {
            assertEquals(Path.of("/profiles/Ada/AppData/Local/Coregnition"),
                    PlatformPaths.dataDirectory("Windows 11", "/profiles/Ada", local));
        }
    }
    @Test void linuxRetainsExistingDataDirectory() {
        assertEquals(Path.of("/home/ada/.local/share/coregnition"),
                PlatformPaths.dataDirectory("Linux", "/home/ada", "/ignored"));
    }
    @Test void bundledJavaUsesPlatformExecutableName() {
        assertEquals(Path.of("/runtime/bin/java.exe"), PlatformPaths.javaExecutable("Windows 11", "/runtime"));
        assertEquals(Path.of("/runtime/bin/java"), PlatformPaths.javaExecutable("Linux", "/runtime"));
        assertEquals(Path.of("/runtime/bin/java"), PlatformPaths.javaExecutable("Darwin", "/runtime"));
    }
}
