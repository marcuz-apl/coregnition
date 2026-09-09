package io.github.marcuzapl.coregnition.desktop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

final class DesktopWorkspace extends BorderPane {
    private final BackendApi api;
    private final Stage stage;
    private final ListView<BackendApi.Project> projects = new ListView<>();
    private final ListView<BackendApi.Asset> assets = new ListView<>();
    private final ListView<BackendApi.Segment> segments = new ListView<>();
    private final CoreImageViewer viewer = new CoreImageViewer();
    private final Label status = new Label("Connecting…"), annotation = new Label("Select an interval to describe");
    private final TextField start = new TextField(), end = new TextField();
    private final ComboBox<String> orientation = new ComboBox<>(), label = new ComboBox<>(), review = new ComboBox<>();
    private final BorderPane controls = new BorderPane();
    private BackendApi.Workspace workspace;
    private boolean updating;

    DesktopWorkspace(Stage stage, BackendApi api) {
        this.stage = stage; this.api = api;
        orientation.getItems().setAll("top-to-bottom", "bottom-to-top", "left-to-right", "right-to-left");
        orientation.getSelectionModel().selectFirst();
        label.getItems().setAll("limestone", "dolostone", "carbonaceous shale", "unknown", "mixed", "unassessable");
        label.getSelectionModel().selectFirst();
        review.getItems().setAll("UNREVIEWED", "REVIEWED"); review.getSelectionModel().selectFirst();
        start.setPromptText("Start depth (ft)"); end.setPromptText("End depth (ft)");
        projects.setPlaceholder(new Label("Create or import a project"));
        assets.setPlaceholder(new Label("Import core images"));
        segments.setPlaceholder(new Label("Select an image and add a depth interval"));
        Button create = button("New project", this::createProject);
        Button refresh = button("Refresh", () -> refreshProjects(null));
        Button importZip = button("Import project ZIP", this::importArchive);
        Button importImage = button("Import images", this::importImages);
        Button csv = button("Export CSV", () -> export(false));
        Button zip = button("Export project ZIP", () -> export(true));
        controls.setTop(new ToolBar(create, refresh, importZip, new Separator(), importImage, csv, zip));
        VBox navigation = new VBox(8, new Label("Projects / wells"), projects, new Label("Images"), assets);
        navigation.setPadding(new Insets(10)); navigation.setPrefWidth(220);
        VBox.setVgrow(projects, Priority.ALWAYS); VBox.setVgrow(assets, Priority.ALWAYS);
        GridPane depths = new GridPane(); depths.setHgap(6); depths.setVgap(6);
        depths.addRow(0, fieldLabel("Start (ft)", start), start);
        depths.addRow(1, fieldLabel("End (ft)", end), end);
        depths.addRow(2, fieldLabel("Orientation", orientation), orientation);
        annotation.setWrapText(true);
        VBox editor = new VBox(9, new Label("Depth intervals"), segments, depths,
            button("Add interval from selected region", this::addSegment), new Separator(), annotation,
            fieldLabel("Lithology", label), label, fieldLabel("Review state", review), review,
            button("Save annotation", this::saveAnnotation), button("Undo latest saved annotation", this::undo));
        editor.setPadding(new Insets(10)); editor.setPrefWidth(320); VBox.setVgrow(segments, Priority.ALWAYS);
        SplitPane split = new SplitPane(navigation, viewer, editor); split.setDividerPositions(0.18, 0.73);
        controls.setCenter(split);
        setCenter(controls); setBottom(status); BorderPane.setMargin(status, new Insets(8));
        controls.setDisable(true);
        projects.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!updating && b != null) loadWorkspace(b.id());
        });
        assets.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!updating) { updateSegments(); if (b != null) loadImage(b); else viewer.setImage(null); }
        });
        segments.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showAnnotation(b));
        segments.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(BackendApi.Segment value, boolean empty) {
                super.updateItem(value, empty);
                var saved = value == null ? null : annotationFor(value.id());
                setText(empty || value == null ? null : value + "\n" + (saved == null ? "Unlabelled · UNREVIEWED" : saved.label() + " · " + saved.reviewState()));
            }
        });
    }
    void ready() { controls.setDisable(false); refreshProjects(null); }
    void unavailable(String message) { status.setText(message); }
    private static Label fieldLabel(String text, javafx.scene.Node field) { Label result = new Label(text); result.setLabelFor(field); return result; }
    private static Button button(String text, Runnable action) { Button button = new Button(text); button.setOnAction(e -> action.run()); return button; }
    private void createProject() {
        TextInputDialog dialog = new TextInputDialog(); dialog.setTitle("New project"); dialog.setHeaderText("Project / well name"); dialog.initOwner(stage);
        dialog.showAndWait().filter(name -> !name.isBlank()).ifPresent(name -> run("Creating project…", () -> api.createProject(name.trim()), project -> refreshProjects(project.id())));
    }
    private void refreshProjects(String selectedId) {
        String previous = selectedId != null ? selectedId : workspace == null ? null : workspace.project().id();
        run("Loading projects…", api::listProjects, found -> {
            updating = true;
            projects.getItems().setAll(found);
            var selected = found.stream().filter(p -> p.id().equals(previous)).findFirst().orElse(found.isEmpty() ? null : found.getFirst());
            projects.getSelectionModel().select(selected); updating = false;
            if (selected != null) loadWorkspace(selected.id());
        });
    }
    private void loadWorkspace(String projectId) {
        String assetId = workspace != null && workspace.project().id().equals(projectId) && assets.getSelectionModel().getSelectedItem() != null ? assets.getSelectionModel().getSelectedItem().id() : null;
        String segmentId = segments.getSelectionModel().getSelectedItem() == null ? null : segments.getSelectionModel().getSelectedItem().id();
        if (workspace != null && !workspace.project().id().equals(projectId)) {
            workspace = null; updating = true;
            assets.getItems().clear(); segments.getItems().clear(); updating = false;
            viewer.setImage(null);
        }
        run("Opening project…", () -> api.workspace(projectId), found -> {
            workspace = found; updating = true;
            assets.getItems().setAll(found.assets());
            var selected = found.assets().stream().filter(a -> a.id().equals(assetId)).findFirst().orElse(found.assets().isEmpty() ? null : found.assets().getFirst());
            assets.getSelectionModel().select(selected); updating = false;
            updateSegments();
            segments.getItems().stream().filter(s -> s.id().equals(segmentId)).findFirst().ifPresent(s -> segments.getSelectionModel().select(s));
            if (selected != null) loadImage(selected); else viewer.setImage(null);
        });
    }
    private void updateSegments() {
        var asset = assets.getSelectionModel().getSelectedItem();
        segments.getItems().setAll(workspace == null || asset == null ? List.of() : workspace.segments().stream().filter(s -> s.assetId().equals(asset.id())).toList());
        annotation.setText("Select an interval to describe");
    }
    private void loadImage(BackendApi.Asset asset) {
        String projectId = workspace.project().id();
        viewer.setImage(null);
        run("Loading image…", () -> {
            var decoded = ImageIO.read(new ByteArrayInputStream(api.image(projectId, asset.id())));
            if (decoded == null) throw new IOException("Cannot display this image");
            return SwingFXUtils.toFXImage(decoded, null);
        }, image -> {
            viewer.setImage(image);
            showAnnotation(segments.getSelectionModel().getSelectedItem());
        });
    }
    private BackendApi.Annotation annotationFor(String segmentId) {
        return workspace == null ? null : workspace.annotations().stream().filter(a -> a.segmentId().equals(segmentId)).findFirst().orElse(null);
    }
    private void showAnnotation(BackendApi.Segment segment) {
        if (segment == null) { annotation.setText("Select an interval to describe"); return; }
        var saved = annotationFor(segment.id());
        annotation.setText(saved == null ? "No saved label · UNREVIEWED" : saved.label() + " · " + saved.reviewState() + " · revision " + saved.revision());
        label.setValue(saved == null ? "unknown" : saved.label()); review.setValue(saved == null ? "UNREVIEWED" : saved.reviewState());
        viewer.setRegion(segment.regionX() == null ? null : new SourceRegion(segment.regionX(), segment.regionY(), segment.regionWidth(), segment.regionHeight()));
    }
    private boolean requireProject() { if (workspace != null) return true; status.setText("Create or select a project first."); return false; }
    private void importImages() {
        if (!requireProject()) return;
        FileChooser chooser = chooser("Import core photographs", "Images", "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff", "*.PNG", "*.JPG", "*.JPEG", "*.TIF", "*.TIFF");
        var files = chooser.showOpenMultipleDialog(stage); if (files == null) return;
        String projectId = workspace.project().id();
        run("Importing images…", () -> {
            StringBuilder errors = new StringBuilder();
            for (var file : files) try { api.importAsset(projectId, file.toPath()); } catch (Exception e) { errors.append(file.getName()).append(": ").append(e.getMessage()).append('\n'); }
            return errors.toString();
        }, errors -> {
            if (!errors.isEmpty()) { Alert alert = new Alert(Alert.AlertType.WARNING, errors); alert.initOwner(stage); alert.setHeaderText("Some images could not be imported"); alert.showAndWait(); }
            loadWorkspace(projectId);
        });
    }
    private void importArchive() {
        var file = chooser("Import project", "Project ZIP", "*.zip").showOpenDialog(stage); if (file == null) return;
        run("Importing project archive…", () -> api.importArchive(file.toPath()), found -> refreshProjects(found.project().id()));
    }
    private void addSegment() {
        if (!requireProject()) return;
        var asset = assets.getSelectionModel().getSelectedItem(); if (asset == null) { status.setText("Select an image first."); return; }
        try {
            double from = Double.parseDouble(start.getText()), to = Double.parseDouble(end.getText());
            if (!Double.isFinite(from) || !Double.isFinite(to) || from < 0 || to <= from) throw new IllegalArgumentException();
            var r = viewer.region();
            var input = new BackendApi.SegmentInput(asset.id(), from, to, orientation.getValue(), r == null ? null : r.x(), r == null ? null : r.y(), r == null ? null : r.width(), r == null ? null : r.height());
            String projectId = workspace.project().id();
            run("Saving depth interval…", () -> { api.createSegment(projectId, input); return true; }, ignored -> loadWorkspace(projectId));
        } catch (IllegalArgumentException e) { status.setText("Enter finite depths in feet: start ≥ 0 and end greater than start."); }
    }
    private void saveAnnotation() {
        var segment = segments.getSelectionModel().getSelectedItem(); if (segment == null) { status.setText("Select an interval first."); return; }
        String projectId = workspace.project().id(), lithology = label.getValue(), state = review.getValue();
        run("Saving annotation…", () -> { api.annotate(projectId, segment.id(), lithology, state); return true; }, ignored -> loadWorkspace(projectId));
    }
    private void undo() {
        var segment = segments.getSelectionModel().getSelectedItem(); if (segment == null) { status.setText("Select an interval first."); return; }
        String projectId = workspace.project().id();
        run("Undoing latest annotation…", () -> { api.undo(projectId, segment.id()); return true; }, ignored -> loadWorkspace(projectId));
    }
    private void export(boolean archive) {
        if (!requireProject()) return;
        FileChooser chooser = chooser("Export " + (archive ? "project" : "description"), archive ? "Project ZIP" : "CSV", archive ? "*.zip" : "*.csv");
        chooser.setInitialFileName(archive ? "coregnition-project.zip" : "coregnition-export.csv");
        var file = chooser.showSaveDialog(stage); if (file == null) return;
        String projectId = workspace.project().id();
        run("Exporting…", () -> {
            byte[] bytes = api.export(projectId, archive);
            Path target = file.toPath().toAbsolutePath(), temp = Files.createTempFile(target.getParent(), ".coregnition-", ".tmp");
            try { Files.write(temp, bytes); Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); } finally { Files.deleteIfExists(temp); }
            return target;
        }, path -> status.setText("Exported to " + path));
    }
    private static FileChooser chooser(String title, String description, String... extensions) {
        FileChooser chooser = new FileChooser(); chooser.setTitle(title); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions)); return chooser;
    }
    private <T> void run(String message, Callable<T> work, Consumer<T> success) {
        controls.setDisable(true); status.setText(message);
        Thread.startVirtualThread(() -> {
            try {
                T result = work.call();
                Platform.runLater(() -> { controls.setDisable(false); status.setText("Ready"); success.accept(result); });
            } catch (Exception e) {
                Platform.runLater(() -> { controls.setDisable(false); status.setText(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()); });
            }
        });
    }
}
