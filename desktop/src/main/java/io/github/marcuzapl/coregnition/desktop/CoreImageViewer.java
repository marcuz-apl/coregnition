package io.github.marcuzapl.coregnition.desktop;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

final class CoreImageViewer extends BorderPane {
    private final ImageView image = new ImageView();
    private final Rectangle overlay = new Rectangle();
    private final Group pixels = new Group(image, overlay);
    private final Group transformed = new Group(pixels);
    private final ScrollPane scroll = new ScrollPane(new Group(transformed));
    private final ToggleButton select = new ToggleButton("Select region");
    private final Label details = new Label("Import or select an image");
    private SourceRegion region;
    private Point2D start;
    private double scale = 1;

    CoreImageViewer() {
        overlay.setFill(Color.color(0.1, 0.6, 1, 0.18));
        overlay.setStroke(Color.DODGERBLUE);
        overlay.setMouseTransparent(true);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #202b35;");
        Button zoomIn = new Button("Zoom +"), zoomOut = new Button("Zoom −"), rotate = new Button("Rotate 90°"), reset = new Button("Reset view"), clear = new Button("Clear region");
        zoomIn.setOnAction(e -> zoom(scale * 1.25));
        zoomOut.setOnAction(e -> zoom(scale / 1.25));
        rotate.setOnAction(e -> transformed.setRotate((transformed.getRotate() + 90) % 360));
        reset.setOnAction(e -> resetView());
        clear.setOnAction(e -> setRegion(null));
        select.selectedProperty().addListener((o, a, b) -> scroll.setPannable(!b));
        image.setOnMousePressed(e -> {
            if (select.isSelected()) { start = image.sceneToLocal(e.getSceneX(), e.getSceneY()); e.consume(); }
        });
        image.setOnMouseDragged(e -> {
            if (select.isSelected() && start != null && image.getImage() != null) {
                Point2D end = image.sceneToLocal(e.getSceneX(), e.getSceneY());
                try { setRegion(SourceRegion.fromDrag(start.getX(), start.getY(), end.getX(), end.getY(), (int) image.getImage().getWidth(), (int) image.getImage().getHeight())); }
                catch (IllegalArgumentException ignored) { }
                e.consume();
            }
        });
        image.setOnMouseReleased(e -> start = null);
        setTop(new ToolBar(zoomOut, zoomIn, rotate, reset, select, clear));
        setCenter(scroll);
        setBottom(details);
        setMinSize(200, 200);
    }
    void setImage(Image value) {
        image.setImage(value);
        setRegion(null);
        resetView();
    }
    private void resetView() {
        transformed.setRotate(0);
        Image value = image.getImage();
        zoom(value == null ? 1 : Math.min(1, Math.max(200, scroll.getViewportBounds().getWidth() - 20) / value.getWidth()));
        scroll.setHvalue(0); scroll.setVvalue(0);
    }
    private void zoom(double value) {
        scale = Math.max(0.02, Math.min(8, value));
        pixels.setScaleX(scale); pixels.setScaleY(scale);
        overlay.setStrokeWidth(2 / scale);
    }
    SourceRegion region() { return region; }
    void setRegion(SourceRegion value) {
        region = value;
        overlay.setVisible(value != null);
        if (value != null) {
            overlay.setX(value.x()); overlay.setY(value.y()); overlay.setWidth(value.width()); overlay.setHeight(value.height());
            details.setText("Source region: x=" + value.x() + ", y=" + value.y() + ", " + value.width() + " × " + value.height() + " pixels");
        } else details.setText(image.getImage() == null ? "Import or select an image" : "Whole image · Original preserved · Drag to pan, or enable Select region");
    }
}
