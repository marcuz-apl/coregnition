package io.github.marcuzapl.coregnition.backend.image;

public final class OpenCvProbe {
    private OpenCvProbe() {}

    public static String status() {
        try {
            Class<?> core = Class.forName("org.opencv.core.Core");
            String nativeLibraryName = (String) core.getField("NATIVE_LIBRARY_NAME").get(null);
            System.loadLibrary(nativeLibraryName);
            return "loaded";
        } catch (ClassNotFoundException exception) {
            return "binding-not-installed";
        } catch (ReflectiveOperationException | UnsatisfiedLinkError exception) {
            return "native-library-not-loaded";
        }
    }
}
