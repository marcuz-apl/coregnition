package io.github.marcuzapl.coregnition.desktop;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;

final class DesktopUiCheck {
    static void verify(BackendApi api, String projectId, String segmentId) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS));
        Stage stage = fx(() -> {
            Stage result = new Stage();
            DesktopWorkspace workspace = new DesktopWorkspace(result, api);
            result.setScene(new Scene(workspace, 1280, 820)); result.show(); workspace.ready();
            return result;
        });
        try {
            await(() -> fx(() -> stage.getScene().getRoot().lookupAll(".list-view").stream()
                .filter(n -> n instanceof ListView<?> list && !list.isDisabled())
                .anyMatch(n -> ((ListView<?>) n).getItems().stream().anyMatch(i -> i instanceof BackendApi.Segment))));
            fx(() -> {
                for (var node : stage.getScene().getRoot().lookupAll(".list-view")) {
                    ListView<?> list = (ListView<?>) node;
                    for (int i = 0; i < list.getItems().size(); i++)
                        if (list.getItems().get(i) instanceof BackendApi.Project p && p.id().equals(projectId)) list.getSelectionModel().select(i);
                }
                return null;
            });
            await(() -> fx(() -> stage.getScene().getRoot().lookupAll(".list-view").stream()
                .filter(n -> !n.isDisabled()).map(n -> (ListView<?>) n)
                .anyMatch(list -> list.getItems().stream().anyMatch(i -> i instanceof BackendApi.Segment s && s.id().equals(segmentId)))));
            fx(() -> {
                for (var node : stage.getScene().getRoot().lookupAll(".list-view")) {
                    ListView<?> list = (ListView<?>) node;
                    for (int i = 0; i < list.getItems().size(); i++) {
                        if (list.getItems().get(i) instanceof BackendApi.Segment s && s.id().equals(segmentId)) list.getSelectionModel().select(i);
                    }
                }
                for (var node : stage.getScene().getRoot().lookupAll(".combo-box")) {
                    @SuppressWarnings("unchecked") ComboBox<String> combo = (ComboBox<String>) node;
                    if (combo.getItems().contains("mixed")) combo.setValue("mixed");
                }
                stage.getScene().getRoot().lookupAll(".button").stream().map(n -> (Button) n).filter(b -> b.getText().equals("Save annotation")).findFirst().orElseThrow().fire();
                return null;
            });
            await(() -> api.workspace(projectId).annotations().stream().anyMatch(a -> a.segmentId().equals(segmentId) && a.label().equals("mixed")));
            await(() -> fx(() -> stage.getScene().getRoot().lookupAll(".button").stream().map(n -> (Button) n).anyMatch(b -> b.getText().equals("Save annotation") && !b.isDisabled())));
            fx(() -> {
                ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png", Path.of("target/desktop-smoke.png").toFile());
                return null;
            });
        } finally { fx(() -> { stage.close(); return null; }); Platform.exit(); }
    }
    private static <T> T fx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work); Platform.runLater(task); return task.get(15, TimeUnit.SECONDS);
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) { if (condition.call()) return; Thread.sleep(100); }
        fail("Desktop workflow did not become ready within 30 seconds");
    }
}
