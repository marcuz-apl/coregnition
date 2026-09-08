package io.github.marcuzapl.coregnition.backend.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class PngMetadataReader {
    private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private PngMetadataReader() {}

    public static PngMetadata read(Path image) throws IOException {
        try (InputStream input = Files.newInputStream(image)) {
            byte[] header = input.readNBytes(29);
            if (header.length != 29 || !Arrays.equals(SIGNATURE, Arrays.copyOf(header, 8))) {
                throw new IOException("Not a PNG file: " + image);
            }
            if (header[12] != 'I' || header[13] != 'H' || header[14] != 'D' || header[15] != 'R') {
                throw new IOException("PNG is missing an IHDR header: " + image);
            }
            int width = integer(header, 16);
            int height = integer(header, 20);
            if (width <= 0 || height <= 0) {
                throw new IOException("PNG dimensions must be positive: " + image);
            }
            return new PngMetadata(width, height, Byte.toUnsignedInt(header[24]), Byte.toUnsignedInt(header[25]));
        }
    }

    private static int integer(byte[] bytes, int offset) {
        return (Byte.toUnsignedInt(bytes[offset]) << 24) | (Byte.toUnsignedInt(bytes[offset + 1]) << 16)
                | (Byte.toUnsignedInt(bytes[offset + 2]) << 8) | Byte.toUnsignedInt(bytes[offset + 3]);
    }
}
