# M0 Linux x64 verification

M0 targets Linux x64. The project builds a loopback-only Spring Boot backend and a JavaFX launcher. The launcher starts the backend when given a packaged backend JAR, polls `GET /api/v1/health`, and terminates its child process when it exits.

## Build and local checks

```sh
./mvnw verify
java -cp backend/target/classes io.github.marcuzapl.coregnition.backend.image.PngInspectionCli /path/to/supplied-core-image-a.png
java -cp backend/target/classes io.github.marcuzapl.coregnition.backend.image.PngInspectionCli /path/to/supplied-core-image-b.png
```

Run the backend with `./mvnw -pl backend spring-boot:run`, then launch the desktop client with `./mvnw -pl desktop javafx:run`. To make the launcher manage the backend process, package the backend and pass `--backend-jar=/absolute/path/to/coregnition-backend-0.0.1-SNAPSHOT.jar`.

## Official OpenCV Java binding check

For the current Ubuntu-based Linux x64 development check, install the Ubuntu `libopencv-java` package. The verified candidate is `4.6.0+dfsg-13.1ubuntu1`; it supplies the OpenCV Java binding and pulls its required native shared libraries. This changes the local development environment and is not part of the repository or production installer.

```sh
sudo apt-get install -y libopencv-java
java -Djava.library.path=/usr/lib/jni -cp backend/target/classes:/usr/share/java/opencv4/opencv-460.jar io.github.marcuzapl.coregnition.backend.image.OpenCvProbeCli /path/to/supplied-core-image.png
```

The command must show that `org.opencv.core.Core` loads and that OpenCV decodes each supplied PNG fixture. The production Linux package must instead bundle matching native libraries. Do not substitute a different binding without updating the architecture decision.

The current automated build validates the backend, JavaFX compilation and PNG header parsing. It cannot claim OpenCV native loading until the local Ubuntu package is installed and the probe succeeds.
