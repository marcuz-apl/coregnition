package io.github.marcuzapl.coregnition.backend.image;

public record PngMetadata(int width, int height, int bitDepth, int colorType) {
    public boolean hasAlphaChannel() {
        return colorType == 4 || colorType == 6;
    }
}
