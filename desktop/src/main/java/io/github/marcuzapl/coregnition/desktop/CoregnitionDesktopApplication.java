package io.github.marcuzapl.coregnition.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CoregnitionDesktopApplication extends Application {
    private final BackendProcess backend = new BackendProcess();

    @Override public void start(Stage stage) {
        Runtime.getRuntime().addShutdownHook(new Thread(backend::close, "coregnition-backend-shutdown"));
        Parameters parameters = getParameters();
        int port = Integer.parseInt(parameters.getNamed().getOrDefault("port", "3041"));
        String backendJar = parameters.getNamed().get("backend-jar");
        DesktopWorkspace workspace = new DesktopWorkspace(stage, new BackendApi("http://127.0.0.1:" + port));
        stage.setScene(new Scene(workspace, 1280, 820));
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setTitle("Coregnition — Manual core description");
        stage.show();
        Thread.startVirtualThread(() -> {
            try {
                if (backendJar != null) backend.start(Path.of(backendJar), port, Path.of(parameters.getNamed().getOrDefault("data-dir", "data")));
                waitForHealth(port);
                Platform.runLater(workspace::ready);
            } catch (Exception exception) {
                Platform.runLater(() -> workspace.unavailable("Backend unavailable: " + exception.getMessage()));
            }
        });
    }

    @Override public void stop() { backend.close(); }

    private static void waitForHealth(int port) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build();
        URI health = URI.create("http://127.0.0.1:" + port + "/api/v1/health");
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                HttpResponse<Void> response = client.send(HttpRequest.newBuilder(health).timeout(java.time.Duration.ofSeconds(2)).GET().build(), HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) return;
            } catch (java.io.IOException ignored) { }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Health endpoint did not become ready at " + health);
    }
}
