package io.github.marcuzapl.coregnition.backend.image;

import java.nio.file.Path;

public final class PngInspectionCli {
    private PngInspectionCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: PngInspectionCli <image.png>");
        PngMetadata metadata = PngMetadataReader.read(Path.of(args[0]));
        System.out.printf("PNG %dx%d, %d-bit, color type %d, alpha=%s%n", metadata.width(), metadata.height(), metadata.bitDepth(), metadata.colorType(), metadata.hasAlphaChannel());
    }
}
