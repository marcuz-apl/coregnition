package io.github.marcuzapl.coregnition.backend.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.marcuzapl.coregnition.backend.workflow.AnnotationRecord;
import io.github.marcuzapl.coregnition.backend.workflow.AssetRecord;
import io.github.marcuzapl.coregnition.backend.workflow.CreateSegmentRequest;
import io.github.marcuzapl.coregnition.backend.workflow.JobRecord;
import io.github.marcuzapl.coregnition.backend.workflow.JobStatus;
import io.github.marcuzapl.coregnition.backend.workflow.PredictionRecord;
import io.github.marcuzapl.coregnition.backend.workflow.ProjectRecord;
import io.github.marcuzapl.coregnition.backend.workflow.ProjectService;
import io.github.marcuzapl.coregnition.backend.workflow.SegmentRecord;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class JobServiceTest {
    private static final Path TEST_ROOT;

    static {
        try {
            TEST_ROOT = Files.createTempDirectory("coregnition-m2-test-");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired
    private ProjectService service;

    @Autowired
    private JobService jobService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("coregnition.database", () -> TEST_ROOT.resolve("metadata.db").toString());
        registry.add("coregnition.storage-root", () -> TEST_ROOT.resolve("projects").toString());
    }

    @Test
    void runsAnalysisJobAndStoresPredictionProvenance() throws Exception {
        ProjectRecord project = service.createProject("Analysis Test Well");
        AssetRecord asset = service.importAsset(project.id(), new MockMultipartFile("file", "test-core.png", "image/png", darkShalePng()));
        SegmentRecord segment = service.createSegment(project.id(), new CreateSegmentRequest(asset.id(), 10.0, 12.0, "TOP_TO_BOTTOM", 0, 0, 60, 60));

        JobRecord job = service.startAnalysis(project.id(), null);
        assertNotNull(job);
        assertEquals(project.id(), job.projectId());

        // Wait for async job to finish (max 5 seconds)
        long deadline = System.currentTimeMillis() + 5000;
        JobRecord current = job;
        while (System.currentTimeMillis() < deadline && (current.status() == JobStatus.QUEUED || current.status() == JobStatus.RUNNING)) {
            Thread.sleep(100);
            current = service.getJob(project.id(), job.id()).orElse(current);
        }

        assertEquals(JobStatus.SUCCEEDED, current.status());
        assertEquals(1.0, current.progress());

        List<PredictionRecord> predictions = service.listPredictions(project.id());
        assertFalse(predictions.isEmpty());
        PredictionRecord pred = predictions.getFirst();
        assertEquals(segment.id(), pred.segmentId());
        assertEquals("carbonaceous shale", pred.suggestedLabel());
        assertEquals(TextureColorClassifier.MODEL_ID, pred.modelId());
        assertNotNull(pred.classScores());
        assertTrue(pred.confidence() > 0.40);

        // Accept suggestion and verify non-destructive annotation creation
        AnnotationRecord annotation = service.acceptPrediction(project.id(), segment.id(), pred.id());
        assertNotNull(annotation);
        assertEquals("carbonaceous shale", annotation.label());
        assertEquals("REVIEWED", annotation.reviewState());

        // Verify CSV export includes reviewed interval with provenance
        String csv = service.exportCsv(project.id(), true);
        assertTrue(csv.contains("carbonaceous shale"));
        assertTrue(csv.contains("REVIEWED"));
    }

    private static byte[] darkShalePng() throws IOException {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(30, 32, 35));
        g.fillRect(0, 0, 80, 80);
        g.setColor(new Color(45, 48, 50));
        for (int y = 0; y < 80; y += 4) {
            g.drawLine(0, y, 80, y);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
