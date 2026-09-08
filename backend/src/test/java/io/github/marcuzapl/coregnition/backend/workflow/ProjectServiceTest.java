package io.github.marcuzapl.coregnition.backend.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
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

    @Test
    void rejectsOverlappingIntervalsOnTheSameImage() throws Exception {
        ProjectRecord project = service.createProject("Overlap validation");
        AssetRecord asset = service.importAsset(project.id(), new MockMultipartFile("file", "overlap.png", "image/png", png()));
        service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 10.0, 12.0, "TOP_TO_BOTTOM"));

        assertThrows(IllegalArgumentException.class, () -> service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 11.0, 13.0, "TOP_TO_BOTTOM")));
    }

    @Test
    void undoingLatestAnnotationRestoresThePreviousRevision() throws Exception {
        ProjectRecord project = service.createProject("Revision undo");
        AssetRecord asset = service.importAsset(project.id(), new MockMultipartFile("file", "revision.png", "image/png", png()));
        SegmentRecord segment = service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 1.0, 2.0, "TOP_TO_BOTTOM"));
        service.annotate(project.id(), segment.id(), new CreateAnnotationRequest("limestone", "UNREVIEWED"));
        service.annotate(project.id(), segment.id(), new CreateAnnotationRequest("dolostone", "REVIEWED"));

        AnnotationRecord restored = service.undoLatestAnnotation(project.id(), segment.id()).orElseThrow();

        assertEquals(1, restored.revision());
        assertEquals("limestone", restored.label());
        assertEquals("UNREVIEWED", restored.reviewState());
    }

    @Test
    void listsProjectsAndReloadsWorkspaceRecords() throws Exception {
        ProjectRecord project = service.createProject("Reloadable well");
        AssetRecord asset = service.importAsset(project.id(), new MockMultipartFile("file", "reload.png", "image/png", png()));
        SegmentRecord segment = service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 2.0, 3.0, "TOP_TO_BOTTOM"));
        AnnotationRecord annotation = service.annotate(project.id(), segment.id(), new CreateAnnotationRequest("dolostone", "REVIEWED"));

        assertTrue(service.listProjects().stream().anyMatch(item -> item.id().equals(project.id())));
        ProjectWorkspace workspace = service.workspace(project.id());
        assertEquals(project.id(), workspace.project().id());
        assertEquals(1, workspace.assets().size());
        assertEquals(asset.id(), workspace.assets().getFirst().id());
        assertEquals(segment.id(), workspace.segments().getFirst().id());
        assertEquals(annotation.id(), workspace.annotations().getFirst().id());
        assertEquals("dolostone", workspace.annotations().getFirst().label());
    }

    @Test
    void exportsSelfContainedProjectArchive() throws Exception {
        ProjectRecord project = service.createProject("Archive well");
        AssetRecord asset = service.importAsset(project.id(), new MockMultipartFile("file", "archive.png", "image/png", png()));
        SegmentRecord segment = service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 4.0, 5.0, "TOP_TO_BOTTOM"));
        service.annotate(project.id(), segment.id(), new CreateAnnotationRequest("limestone", "REVIEWED"));

        byte[] archive = service.exportArchive(project.id());
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            assertEquals("manifest.json", zip.getNextEntry().getName());
            String manifest = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(manifest.contains("Archive well"));
            assertTrue(manifest.contains("limestone"));
            assertEquals("assets/" + asset.id() + ".png", zip.getNextEntry().getName());
        }
    }

    @Test
    void restoresArchiveIntoANewProject() throws Exception {
        ProjectRecord source = service.createProject("Portable well");
        AssetRecord asset = service.importAsset(source.id(), new MockMultipartFile("file", "portable.png", "image/png", png()));
        SegmentRecord segment = service.createSegment(source.id(), new CreateSegmentRequest(asset.id(), 7.0, 8.5, "BOTTOM_TO_TOP"));
        service.annotate(source.id(), segment.id(), new CreateAnnotationRequest("carbonaceous shale", "REVIEWED"));

        ProjectWorkspace restored = service.importArchive(new MockMultipartFile("archive", "portable-project.zip", "application/zip", service.exportArchive(source.id())));

        assertEquals("Portable well", restored.project().name());
        assertEquals(1, restored.assets().size());
        assertEquals(1, restored.segments().size());
        assertEquals(7.0, restored.segments().getFirst().startDepthFeet());
        assertEquals("carbonaceous shale", restored.annotations().getFirst().label());
        assertTrue(Files.isRegularFile(service.assetPath(restored.project().id(), restored.assets().getFirst().id())));
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_4BYTE_ABGR);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
