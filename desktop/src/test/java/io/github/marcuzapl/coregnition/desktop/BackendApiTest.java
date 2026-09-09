package io.github.marcuzapl.coregnition.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class BackendApiTest {
    @Test void readsReorderedFieldsAndEscapedNames() throws Exception {
        try (Fixture f = new Fixture(200, "[{\"name\":\"Well \\\"A\\\" – شرق\",\"createdAt\":\"now\",\"id\":\"p-1\"}]")) {
            var projects = f.api.listProjects();
            assertEquals(1, projects.size());
            assertEquals("Well \"A\" – شرق", projects.getFirst().name());
        }
    }
    @Test void surfacesBackendValidationErrors() throws Exception {
        try (Fixture f = new Fixture(400, "{\"message\":\"Depth intervals overlap\"}")) {
            assertTrue(assertThrows(Exception.class, () -> f.api.listProjects()).getMessage().contains("Depth intervals overlap"));
        }
    }
    @Test void createsProjectWithJsonEscaping() throws Exception {
        try (Fixture f = new Fixture(200, "{\"id\":\"p-1\",\"name\":\"Pilot\"}")) {
            assertEquals("p-1", f.api.createProject("Well \"A\"").id());
            assertEquals("POST /api/v1/projects", f.request);
            assertEquals("{\"name\":\"Well \\\"A\\\"\"}", f.body);
        }
    }
    @Test void uploadsFileAsMultipartAndAcceptsEmptyUndo() throws Exception {
        var file = Files.createTempFile("core-upload", ".png");
        try (Fixture f = new Fixture(200, "{}")) {
            Files.write(file, new byte[]{1, 2, 3});
            f.api.importAsset("p-1", file);
            assertEquals("POST /api/v1/projects/p-1/assets", f.request);
            assertTrue(f.body.contains("name=\"file\""));
            assertTrue(f.body.contains("\r\n\r\n\u0001\u0002\u0003\r\n"));
        } finally { Files.delete(file); }
        try (Fixture f = new Fixture(204, "")) {
            f.api.undo("p-1", "s-1");
            assertEquals("DELETE /api/v1/projects/p-1/segments/s-1/annotations/latest", f.request);
        }
    }
    private static class Fixture implements AutoCloseable {
        final HttpServer server;
        final BackendApi api;
        volatile String request, body;
        Fixture(int status, String response) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                request = exchange.getRequestMethod() + " " + exchange.getRequestURI();
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
                if (status != 204) exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            api = new BackendApi("http://127.0.0.1:" + server.getAddress().getPort());
        }
        public void close() { server.stop(0); }
    }
}
