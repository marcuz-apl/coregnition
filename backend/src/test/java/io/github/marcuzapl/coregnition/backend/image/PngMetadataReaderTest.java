package io.github.marcuzapl.coregnition.backend.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PngMetadataReaderTest {
    @TempDir Path temporaryDirectory;

    @Test
    void readsRgbaPngHeader() throws Exception {
        Path image = temporaryDirectory.resolve("core.png");
        Files.write(image, pngHeader(882, 1595, 8, 6));
        PngMetadata metadata = PngMetadataReader.read(image);
        assertEquals(882, metadata.width());
        assertEquals(1595, metadata.height());
        assertEquals(8, metadata.bitDepth());
        assertEquals(6, metadata.colorType());
        assertEquals(true, metadata.hasAlphaChannel());
    }

    @Test
    void rejectsNonPngInput() throws Exception {
        Path image = temporaryDirectory.resolve("core.txt");
        Files.writeString(image, "not an image");
        assertThrows(java.io.IOException.class, () -> PngMetadataReader.read(image));
    }

    private static byte[] pngHeader(int width, int height, int bitDepth, int colorType) {
        byte[] header = new byte[29];
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        System.arraycopy(signature, 0, header, 0, signature.length);
        header[11] = 13; header[12] = 'I'; header[13] = 'H'; header[14] = 'D'; header[15] = 'R';
        writeInteger(header, 16, width); writeInteger(header, 20, height);
        header[24] = (byte) bitDepth; header[25] = (byte) colorType;
        return header;
    }

    private static void writeInteger(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24); bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8); bytes[offset + 3] = (byte) value;
    }
}
