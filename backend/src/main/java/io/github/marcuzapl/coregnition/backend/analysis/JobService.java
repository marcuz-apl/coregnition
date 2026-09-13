package io.github.marcuzapl.coregnition.backend.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.marcuzapl.coregnition.backend.persistence.ProjectNotFoundException;
import io.github.marcuzapl.coregnition.backend.persistence.ProjectStore;
import io.github.marcuzapl.coregnition.backend.workflow.AssetRecord;
import io.github.marcuzapl.coregnition.backend.workflow.JobRecord;
import io.github.marcuzapl.coregnition.backend.workflow.JobStatus;
import io.github.marcuzapl.coregnition.backend.workflow.PredictionRecord;
import io.github.marcuzapl.coregnition.backend.workflow.ProjectRecord;
import io.github.marcuzapl.coregnition.backend.workflow.SegmentRecord;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final ProjectStore store;
    private final LithologyClassifier classifier;
    private final ObjectMapper mapper;
    private final ExecutorService executor;
    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    public JobService(ProjectStore store, LithologyClassifier classifier, ObjectMapper mapper) {
        this.store = store;
        this.classifier = classifier;
        this.mapper = mapper;
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "coregnition-job-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public JobRecord startAnalysis(String projectId, String segmentId, java.util.function.BiFunction<String, String, Path> assetPathResolver) {
        ProjectRecord project = store.project(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        JobRecord job = store.createJob(project.id(), "LITHOLOGY_INTERPRETATION");

        Future<?> task = executor.submit(() -> runAnalysisTask(job.id(), projectId, segmentId, assetPathResolver));
        activeTasks.put(job.id(), task);

        return job;
    }

    private void runAnalysisTask(String jobId, String projectId, String targetSegmentId, java.util.function.BiFunction<String, String, Path> assetPathResolver) {
        try {
            store.updateJobStatus(jobId, JobStatus.RUNNING, 0.0, null, null);

            List<SegmentRecord> segments;
            if (targetSegmentId != null && !targetSegmentId.isBlank()) {
                SegmentRecord segment = store.segment(projectId, targetSegmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + targetSegmentId));
                segments = List.of(segment);
            } else {
                segments = store.segments(projectId);
            }

            if (segments.isEmpty()) {
                store.updateJobStatus(jobId, JobStatus.SUCCEEDED, 1.0, null, Instant.now().toString());
                return;
            }

            int total = segments.size();
            for (int i = 0; i < total; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    store.updateJobStatus(jobId, JobStatus.CANCELLED, (double) i / total, "Cancelled by user", Instant.now().toString());
                    return;
                }

                SegmentRecord segment = segments.get(i);
                AssetRecord asset = store.asset(projectId, segment.assetId())
                    .orElseThrow(() -> new IllegalStateException("Asset not found for segment: " + segment.id()));

                Path imagePath = assetPathResolver.apply(projectId, segment.assetId());
                BufferedImage fullImage = ImageIO.read(imagePath.toFile());
                if (fullImage == null) {
                    throw new IllegalStateException("Unable to decode asset image: " + asset.originalName());
                }

                BufferedImage cropped = cropSegment(fullImage, segment);
                PredictionResult result = classifier.classify(cropped);

                String scoresJson;
                try {
                    scoresJson = mapper.writeValueAsString(result.classScores());
                } catch (JsonProcessingException e) {
                    scoresJson = "{}";
                }

                store.createPrediction(
                    projectId,
                    jobId,
                    segment.id(),
                    result.suggestedLabel(),
                    result.confidence(),
                    scoresJson,
                    result.isUnknown(),
                    result.modelId(),
                    result.modelChecksum(),
                    result.preprocessingVersion(),
                    asset.sha256()
                );

                double progress = Math.min(1.0, (double) (i + 1) / total);
                store.updateJobStatus(jobId, JobStatus.RUNNING, progress, null, null);
            }

            store.updateJobStatus(jobId, JobStatus.SUCCEEDED, 1.0, null, Instant.now().toString());
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            store.updateJobStatus(jobId, JobStatus.FAILED, 0.0, e.getMessage(), Instant.now().toString());
        } finally {
            activeTasks.remove(jobId);
        }
    }

    private BufferedImage cropSegment(BufferedImage fullImage, SegmentRecord segment) {
        int width = fullImage.getWidth();
        int height = fullImage.getHeight();

        int cropX = 0;
        int cropY = 0;
        int cropW = width;
        int cropH = height;

        if (segment.regionX() != null && segment.regionY() != null && segment.regionWidth() != null && segment.regionHeight() != null) {
            cropX = Math.max(0, segment.regionX());
            cropY = Math.max(0, segment.regionY());
            cropW = Math.min(segment.regionWidth(), width - cropX);
            cropH = Math.min(segment.regionHeight(), height - cropY);
        }

        if (cropW <= 0 || cropH <= 0) {
            return fullImage;
        }

        return fullImage.getSubimage(cropX, cropY, cropW, cropH);
    }

    public boolean cancelJob(String projectId, String jobId) {
        Future<?> future = activeTasks.get(jobId);
        if (future != null) {
            future.cancel(true);
            store.updateJobStatus(jobId, JobStatus.CANCELLED, 0.0, "Cancelled by user", Instant.now().toString());
            activeTasks.remove(jobId);
            return true;
        }
        Optional<JobRecord> record = store.job(projectId, jobId);
        if (record.isPresent() && (record.get().status() == JobStatus.QUEUED || record.get().status() == JobStatus.RUNNING)) {
            store.updateJobStatus(jobId, JobStatus.CANCELLED, record.get().progress(), "Cancelled by user", Instant.now().toString());
            return true;
        }
        return false;
    }

    public Optional<JobRecord> getJob(String projectId, String jobId) {
        return store.job(projectId, jobId);
    }

    public List<JobRecord> listJobs(String projectId) {
        return store.jobs(projectId);
    }

    public List<PredictionRecord> listPredictions(String projectId) {
        return store.predictions(projectId);
    }

    public List<PredictionRecord> listPredictionsForSegment(String projectId, String segmentId) {
        return store.predictionsForSegment(projectId, segmentId);
    }

    public Optional<PredictionRecord> latestPredictionForSegment(String projectId, String segmentId) {
        return store.latestPredictionForSegment(projectId, segmentId);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
