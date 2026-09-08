package io.github.marcuzapl.coregnition.backend.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class ProjectServiceTest {
    private static final Path TEST_ROOT;

    static {
        try {
            TEST_ROOT = Files.createTempDirectory("coregnition-m1-test-");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired
    private ProjectService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("coregnition.database", () -> TEST_ROOT.resolve("metadata.db").toString());
        registry.add("coregnition.storage-root", () -> TEST_ROOT.resolve("projects").toString());
    }

    @Test
    void persistsImportCalibrationRevisionAndExport() throws Exception {
        ProjectRecord project = service.createProject("Pilot well");
        MockMultipartFile upload = new MockMultipartFile("file", "core.png", "image/png", png());

        AssetRecord asset = service.importAsset(project.id(), upload);
        assertEquals(12, asset.width());
        assertEquals(8, asset.height());
        assertTrue(Files.isRegularFile(service.assetPath(project.id(), asset.id())));
        assertThrows(DuplicateKeyException.class, () -> service.importAsset(project.id(), upload));

        SegmentRecord segment = service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 10.0, 12.5, "TOP_TO_BOTTOM"));
        AnnotationRecord annotation = service.annotate(project.id(), segment.id(), new CreateAnnotationRequest("carbonaceous shale", "REVIEWED"));

        assertEquals(1, annotation.revision());
        assertTrue(service.exportCsv(project.id()).contains("10.0,12.5,\"TOP_TO_BOTTOM\",\"carbonaceous shale\",\"REVIEWED\""));
    }

    @Test
    void rejectsReversedDepthBounds() throws Exception {
        ProjectRecord project = service.createProject("Validation well");
        MockMultipartFile upload = new MockMultipartFile("file", "core.png", "image/png", png());
        AssetRecord asset = service.importAsset(project.id(), upload);

        assertThrows(IllegalArgumentException.class, () -> service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 12.5, 10.0, "TOP_TO_BOTTOM")));
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_4BYTE_ABGR);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
