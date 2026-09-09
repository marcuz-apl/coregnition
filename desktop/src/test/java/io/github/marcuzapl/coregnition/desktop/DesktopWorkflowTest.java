package io.github.marcuzapl.coregnition.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopWorkflowTest {
    @TempDir Path directory;

    @Test void persistsManualWorkflowAndRestoresPortableArchive() throws Exception {
        Path jar = Path.of("../backend/target/coregnition-backend-0.0.1-SNAPSHOT.jar").toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(jar), "Build the backend first with ./mvnw package -DskipTests");
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        BackendProcess process = new BackendProcess();
        process.start(jar, port, directory);
        try {
            BackendApi api = new BackendApi("http://127.0.0.1:" + port);
            boolean ready = false;
            for (int attempt = 0; attempt < 150; attempt++) {
                try { api.listProjects(); ready = true; break; } catch (Exception ignored) { Thread.sleep(200); }
            }
            assertTrue(ready, "Managed desktop backend did not start");
            var project = api.createProject("Well \"A\" – شرق");
            Path png = directory.resolve("core.png");
            ImageIO.write(new BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB), "png", png.toFile());
            api.importAsset(project.id(), png);
            var workspace = api.workspace(project.id());
            var asset = workspace.assets().getFirst();
            assertArrayEquals(Files.readAllBytes(png), api.image(project.id(), asset.id()));
            api.createSegment(project.id(), new BackendApi.SegmentInput(asset.id(), 100, 110, "bottom-to-top", 2, 3, 20, 30));
            var segment = api.workspace(project.id()).segments().getFirst();
            api.annotate(project.id(), segment.id(), "limestone", "UNREVIEWED");
            api.annotate(project.id(), segment.id(), "dolostone", "REVIEWED");
            api.undo(project.id(), segment.id());
            assertEquals("limestone", api.workspace(project.id()).annotations().getFirst().label());
            api.annotate(project.id(), segment.id(), "dolostone", "REVIEWED");
            var reopened = new BackendApi("http://127.0.0.1:" + port).workspace(project.id());
            assertEquals("Well \"A\" – شرق", reopened.project().name());
            assertEquals(2, reopened.segments().getFirst().regionX());
            assertEquals("REVIEWED", reopened.annotations().getFirst().reviewState());
            String csv = new String(api.export(project.id(), false), StandardCharsets.UTF_8);
            assertTrue(csv.contains("\"dolostone\",\"REVIEWED\",\"feet\""));
            assertTrue(csv.contains(asset.sha256()));
            Path archive = directory.resolve("project.zip"); Files.write(archive, api.export(project.id(), true));
            var restored = api.importArchive(archive);
            assertNotEquals(project.id(), restored.project().id());
            assertEquals(100, restored.segments().getFirst().startDepthFeet());
            assertEquals("dolostone", restored.annotations().getFirst().label());
            assertEquals(asset.sha256(), restored.assets().getFirst().sha256());
            assertArrayEquals(Files.readAllBytes(png), api.image(restored.project().id(), restored.assets().getFirst().id()));
            Path largeArchive = directory.resolve("large.zip");
            try (var sparse = new java.io.RandomAccessFile(largeArchive.toFile(), "rw")) { sparse.setLength(110_000_000L); }
            Exception invalidArchive = assertThrows(Exception.class, () -> api.importArchive(largeArchive));
            assertTrue(invalidArchive.getMessage().contains("missing its manifest"), invalidArchive::getMessage);
            assertTrue(assertThrows(Exception.class, () -> api.importAsset(project.id(), largeArchive)).getMessage().contains("100 MB"));
            if (Boolean.getBoolean("coregnition.uiTest")) DesktopUiCheck.verify(api, project.id(), segment.id());
            assertThrows(Exception.class, () -> api.createSegment(project.id(), new BackendApi.SegmentInput(asset.id(), 105, 115, "top-to-bottom", null, null, null, null)));
        } finally {
            process.close();
        }
        assertThrows(Exception.class, () -> new BackendApi("http://127.0.0.1:" + port).listProjects());
        assertThrows(Exception.class, () -> process.start(jar, port, directory));
        assertTrue(Files.isRegularFile(directory.resolve("coregnition.db")));
    }
}
