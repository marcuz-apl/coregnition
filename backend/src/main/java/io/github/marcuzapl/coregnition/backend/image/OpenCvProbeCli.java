package io.github.marcuzapl.coregnition.backend.image;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OpenCvProbeCli {
    private OpenCvProbeCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !Files.isRegularFile(Path.of(args[0]))) {
            throw new IllegalArgumentException("Usage: OpenCvProbeCli <image.png>");
        }
        if (!"loaded".equals(OpenCvProbe.status())) {
            throw new IllegalStateException("OpenCV status: " + OpenCvProbe.status());
        }
        Class<?> imageCodecs = Class.forName("org.opencv.imgcodecs.Imgcodecs");
        Method read = imageCodecs.getMethod("imread", String.class, int.class);
        int unchanged = imageCodecs.getField("IMREAD_UNCHANGED").getInt(null);
        Object image = read.invoke(null, args[0], unchanged);
        boolean empty = (boolean) image.getClass().getMethod("empty").invoke(image);
        if (empty) throw new IllegalStateException("OpenCV could not decode the supplied PNG");
        System.out.println("OpenCV decoded supplied PNG successfully.");
    }
}
