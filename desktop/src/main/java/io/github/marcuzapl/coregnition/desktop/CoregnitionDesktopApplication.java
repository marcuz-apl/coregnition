package io.github.marcuzapl.coregnition.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CoregnitionDesktopApplication extends Application {
    private final BackendProcess backend = new BackendProcess();

    @Override public void start(Stage stage) {
        Parameters parameters = getParameters();
        int port = Integer.parseInt(parameters.getNamed().getOrDefault("port", "8787"));
        String backendJar = parameters.getNamed().get("backend-jar");
        Label status = new Label("Checking local Coregnition backend…");
        stage.setScene(new Scene(new StackPane(status), 480, 180));
        stage.setTitle("Coregnition");
        stage.show();
        Thread.startVirtualThread(() -> {
            try {
                if (backendJar != null) backend.start(Path.of(backendJar), port);
                waitForHealth(port);
                Platform.runLater(() -> status.setText("Coregnition local backend is ready."));
            } catch (Exception exception) {
                Platform.runLater(() -> status.setText("Backend unavailable: " + exception.getMessage()));
            }
        });
    }

    @Override public void stop() { backend.close(); }

    private static void waitForHealth(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URI health = URI.create("http://127.0.0.1:" + port + "/api/v1/health");
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                HttpResponse<Void> response = client.send(HttpRequest.newBuilder(health).GET().build(), HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) return;
            } catch (java.io.IOException ignored) { }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Health endpoint did not become ready at " + health);
    }
}
