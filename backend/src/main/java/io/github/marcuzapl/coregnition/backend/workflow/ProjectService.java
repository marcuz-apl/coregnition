package io.github.marcuzapl.coregnition.backend.workflow;

import io.github.marcuzapl.coregnition.backend.image.PngMetadata;
import io.github.marcuzapl.coregnition.backend.image.PngMetadataReader;
import io.github.marcuzapl.coregnition.backend.persistence.ProjectNotFoundException;
import io.github.marcuzapl.coregnition.backend.persistence.ProjectStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProjectService {
    private static final Set<String> SUPPORTED = Set.of("png", "jpg", "jpeg", "tif", "tiff");
    private static final Set<String> LABELS = Set.of("limestone", "dolostone", "carbonaceous shale", "unknown", "mixed", "unassessable");
    private static final Set<String> REVIEW_STATES = Set.of("UNREVIEWED", "REVIEWED");
    private final ProjectStore store;
    private final Path storageRoot;

    public ProjectService(ProjectStore store, @Value("${coregnition.storage-root:data/projects}") String storageRoot) throws IOException {
        this.store = store;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        Files.createDirectories(this.storageRoot);
    }

    public ProjectRecord createProject(String name) throws IOException {
        if (name == null || name.isBlank() || name.length() > 200) throw new IllegalArgumentException("Project name is required and must be at most 200 characters");
        ProjectRecord project = store.createProject(name.trim());
        Files.createDirectories(projectDirectory(project.id()).resolve("assets"));
        return project;
    }

    public List<ProjectRecord> listProjects() { return store.projects(); }

    public ProjectWorkspace workspace(String projectId) {
        ProjectRecord project = store.project(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        return new ProjectWorkspace(project, store.assets(projectId), store.segments(projectId), store.annotations(projectId));
    }

    public AssetRecord importAsset(String projectId, MultipartFile upload) throws IOException {
        requireProject(projectId);
        String original = safeName(upload.getOriginalFilename());
        String extension = extension(original);
        if (!SUPPORTED.contains(extension)) throw new IllegalArgumentException("Supported image formats are PNG, JPEG and TIFF");
        if (upload.isEmpty() || upload.getSize() > 100_000_000L) throw new IllegalArgumentException("Image must be non-empty and at most 100 MB");
        Path assetDirectory = projectDirectory(projectId).resolve("assets").normalize();
        Files.createDirectories(assetDirectory);
        Path temporary = Files.createTempFile(assetDirectory, ".upload-", ".tmp");
        try {
            upload.transferTo(temporary);
            String digest = sha256(temporary);
            if (store.assetBySha(projectId, digest).isPresent()) throw new DuplicateKeyException("This image is already imported in the project");
            try (InputStream input = Files.newInputStream(temporary)) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(input);
                if (image == null) throw new IllegalArgumentException("The uploaded file is not a decodable image");
                String relative = "assets/" + java.util.UUID.randomUUID() + "." + extension;
                Path target = projectDirectory(projectId).resolve(relative).normalize();
                Files.createDirectories(target.getParent());
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                PngMetadata png = extension.equals("png") ? PngMetadataReader.read(target) : null;
                try {
                    return store.createAsset(projectId, original, relative, digest, image.getWidth(), image.getHeight(), png == null ? null : png.bitDepth(), png == null ? null : png.colorType());
                } catch (RuntimeException failure) {
                    Files.deleteIfExists(target);
                    throw failure;
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public Path assetPath(String projectId, String assetId) {
        AssetRecord asset = store.asset(projectId, assetId).orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        Path path = projectDirectory(projectId).resolve(asset.relativePath()).normalize();
        if (!path.startsWith(projectDirectory(projectId)) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Asset file is unavailable");
        return path;
    }

    public SegmentRecord createSegment(String projectId, CreateSegmentRequest request) {
        requireProject(projectId);
        if (request == null || request.assetId() == null || request.startDepthFeet() == null || request.endDepthFeet() == null || request.orientation() == null || request.orientation().isBlank()) throw new IllegalArgumentException("Asset, depth bounds and orientation are required");
        if (request.startDepthFeet() < 0 || request.endDepthFeet() <= request.startDepthFeet()) throw new IllegalArgumentException("Depths must be non-negative and end depth must be greater than start depth");
        store.asset(projectId, request.assetId()).orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));
        return store.createSegment(projectId, request.assetId(), request.startDepthFeet(), request.endDepthFeet(), request.orientation().trim());
    }

    public AnnotationRecord annotate(String projectId, String segmentId, CreateAnnotationRequest request) {
        requireProject(projectId);
        if (request == null || request.label() == null || !LABELS.contains(request.label().toLowerCase(Locale.ROOT)) || request.reviewState() == null || !REVIEW_STATES.contains(request.reviewState().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Label must be one of the configured lithology labels and reviewState must be REVIEWED or UNREVIEWED");
        return store.annotate(segmentId, request.label().toLowerCase(Locale.ROOT), request.reviewState().toUpperCase(Locale.ROOT));
    }

    public String exportCsv(String projectId) {
        requireProject(projectId);
        StringBuilder csv = new StringBuilder("segment_id,asset_id,original_name,start_depth_feet,end_depth_feet,orientation,label,review_state\n");
        for (ProjectStore.SegmentExportRow row : store.exportRows(projectId)) csv.append(csv(row.id())).append(',').append(csv(row.assetId())).append(',').append(csv(row.originalName())).append(',').append(row.startFeet()).append(',').append(row.endFeet()).append(',').append(csv(row.orientation())).append(',').append(csv(row.label())).append(',').append(csv(row.reviewState())).append('\n');
        return csv.toString();
    }

    private void requireProject(String id) { if (id == null || store.project(id).isEmpty()) throw new ProjectNotFoundException(id); }
    private Path projectDirectory(String id) { return storageRoot.resolve(id).normalize(); }
    private static String safeName(String name) { if (name == null || name.isBlank() || Path.of(name).getFileName().toString().equals(".")) throw new IllegalArgumentException("Image filename is required"); return Path.of(name).getFileName().toString(); }
    private static String extension(String name) { int dot = name.lastIndexOf('.'); if (dot < 1 || dot == name.length() - 1) throw new IllegalArgumentException("Image must have a supported extension"); return name.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private static String sha256(Path path) throws IOException { try (InputStream input = Files.newInputStream(path)) { MessageDigest digest = MessageDigest.getInstance("SHA-256"); input.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest)); return HexFormat.of().formatHex(digest.digest()); } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); } }
    private static String csv(String value) { String safe = value == null ? "" : value; if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe; return "\"" + safe.replace("\"", "\"\"") + "\""; }
}
