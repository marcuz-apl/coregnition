package io.github.marcuzapl.coregnition.desktop;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BackendApi {
    record Project(String id, String name) { @Override public String toString() { return name; } }
    record Asset(String id, String originalName, int width, int height, String sha256) {
        @Override public String toString() { return originalName; }
    }
    record Segment(String id, String assetId, double startDepthFeet, double endDepthFeet, String orientation,
                   Integer regionX, Integer regionY, Integer regionWidth, Integer regionHeight) {
        @Override public String toString() { return startDepthFeet + "–" + endDepthFeet + " ft · " + orientation; }
    }
    record Annotation(String segmentId, String label, String reviewState, int revision) { }
    record Workspace(Project project, List<Asset> assets, List<Segment> segments, List<Annotation> annotations) { }
    record SegmentInput(String assetId, double startDepthFeet, double endDepthFeet, String orientation,
                        Integer regionX, Integer regionY, Integer regionWidth, Integer regionHeight) { }
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    BackendApi(String baseUrl) { this.baseUrl = baseUrl; }
    List<Project> listProjects() throws Exception {
        return List.of(json.readValue(send("GET", "/api/v1/projects", null), Project[].class));
    }
    Project createProject(String name) throws Exception { return decode(post("/api/v1/projects", Map.of("name", name)), Project.class); }
    Workspace workspace(String projectId) throws Exception { return decode(send("GET", projectPath(projectId), null), Workspace.class); }
    void importAsset(String projectId, Path file) throws Exception { upload(projectPath(projectId) + "/assets", file, 100_000_000L); }
    Workspace importArchive(Path file) throws Exception { return decode(upload("/api/v1/projects/archive", file, 300_000_000L), Workspace.class); }
    void createSegment(String projectId, SegmentInput input) throws Exception { post(projectPath(projectId) + "/segments", input); }
    void annotate(String projectId, String segmentId, String label, String reviewState) throws Exception {
        post(projectPath(projectId) + "/segments/" + id(segmentId) + "/annotations", Map.of("label", label, "reviewState", reviewState));
    }
    void undo(String projectId, String segmentId) throws Exception { send("DELETE", projectPath(projectId) + "/segments/" + id(segmentId) + "/annotations/latest", null); }
    byte[] export(String projectId, boolean archive) throws Exception { return send("GET", projectPath(projectId) + (archive ? "/archive.zip" : "/export.csv"), null); }
    byte[] image(String projectId, String assetId) throws Exception { return send("GET", projectPath(projectId) + "/assets/" + id(assetId) + "/content", null); }
    private static String id(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid record identifier");
        return value;
    }
    private static String projectPath(String value) { return "/api/v1/projects/" + id(value); }
    private <T> T decode(byte[] bytes, Class<T> type) throws IOException { return json.readValue(bytes, type); }
    private byte[] post(String path, Object value) throws Exception { return send("POST", path, json.writeValueAsBytes(value)); }
    private HttpRequest.Builder request(String path) { return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofMinutes(2)); }
    private byte[] send(String method, String path, byte[] body) throws Exception {
        return execute(request(path).header("Content-Type", "application/json").method(method,
            body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body)).build());
    }
    private byte[] upload(String path, Path file, long maxBytes) throws Exception {
        if (java.nio.file.Files.size(file) > maxBytes) throw new IOException("Import file exceeds " + maxBytes / 1_000_000 + " MB");
        String boundary = "Coregnition" + UUID.randomUUID();
        String name = file.getFileName().toString().replaceAll("[\\\"\\r\\n\\\\]", "_");
        String prefix = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + name
            + "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
        return execute(request(path).header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(
            HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofString(prefix), HttpRequest.BodyPublishers.ofFile(file),
                HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"))).build());
    }
    private byte[] execute(HttpRequest request) throws Exception {
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detail = new String(response.body(), StandardCharsets.UTF_8);
            try {
                var error = json.readTree(detail);
                if (error.has("message")) detail = error.get("message").asText();
                else if (error.has("error")) detail = error.get("error").asText();
            } catch (IOException ignored) { }
            throw new IOException("Request failed (" + response.statusCode() + "): " + detail.substring(0, Math.min(detail.length(), 500)));
        }
        return response.body();
    }
}
