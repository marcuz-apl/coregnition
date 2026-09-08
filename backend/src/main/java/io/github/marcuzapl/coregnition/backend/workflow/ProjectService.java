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
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProjectService {
    private static final Set<String> SUPPORTED = Set.of("png", "jpg", "jpeg", "tif", "tiff");
    private static final Set<String> LABELS = Set.of("limestone", "dolostone", "carbonaceous shale", "unknown", "mixed", "unassessable");
    private static final Set<String> REVIEW_STATES = Set.of("UNREVIEWED", "REVIEWED");
    private static final long MAX_ARCHIVE_SIZE = 300_000_000L;
    private static final int MAX_ARCHIVE_ENTRIES = 102;
    private final ProjectStore store;
    private final Path storageRoot;
    private final ObjectMapper mapper;

    public ProjectService(ProjectStore store, ObjectMapper mapper, @Value("${coregnition.storage-root:data/projects}") String storageRoot) throws IOException {
        this.store = store;
        this.mapper = mapper;
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

    public byte[] exportArchive(String projectId) throws IOException {
        ProjectWorkspace workspace = workspace(projectId);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(mapper.writeValueAsBytes(workspace));
            zip.closeEntry();
            for (AssetRecord asset : workspace.assets()) {
                zip.putNextEntry(new ZipEntry("assets/" + asset.id() + "." + extension(asset.originalName())));
                Files.copy(assetPath(projectId, asset.id()), zip);
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    public ProjectWorkspace importArchive(MultipartFile archive) throws IOException {
        if (archive == null || archive.isEmpty() || archive.getSize() > MAX_ARCHIVE_SIZE) throw new IllegalArgumentException("Project archive must be non-empty and at most 300 MB");
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(archive.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if ((!name.equals("manifest.json") && !name.startsWith("assets/")) || name.contains("..") || entries.size() >= MAX_ARCHIVE_ENTRIES) throw new IllegalArgumentException("Project archive has an invalid entry");
                byte[] contents = readArchiveEntry(zip, name.equals("manifest.json") ? 1_000_000 : 100_000_000);
                if (entries.putIfAbsent(name, contents) != null) throw new IllegalArgumentException("Project archive contains duplicate entries");
            }
        }
        byte[] manifest = entries.get("manifest.json");
        if (manifest == null) throw new IllegalArgumentException("Project archive is missing its manifest");
        ProjectWorkspace source;
        try { source = mapper.readValue(manifest, ProjectWorkspace.class); }
        catch (Exception exception) { throw new IllegalArgumentException("Project archive manifest is invalid", exception); }
        if (source.project() == null || source.project().name() == null || source.assets() == null || source.segments() == null || source.annotations() == null) throw new IllegalArgumentException("Project archive manifest is incomplete");

        ProjectRecord restored = createProject(source.project().name());
        Map<String, AssetRecord> assets = new HashMap<>();
        for (AssetRecord sourceAsset : source.assets()) {
            String archiveName = "assets/" + sourceAsset.id() + "." + extension(sourceAsset.originalName());
            byte[] image = entries.get(archiveName);
            if (image == null) throw new IllegalArgumentException("Project archive is missing an image asset");
            assets.put(sourceAsset.id(), importAsset(restored.id(), new InMemoryUpload(sourceAsset.originalName(), image)));
        }
        Map<String, SegmentRecord> segments = new HashMap<>();
        for (SegmentRecord sourceSegment : source.segments()) {
            AssetRecord restoredAsset = assets.get(sourceSegment.assetId());
            if (restoredAsset == null) throw new IllegalArgumentException("Project archive has a segment with no image asset");
            segments.put(sourceSegment.id(), createSegment(restored.id(), new CreateSegmentRequest(restoredAsset.id(), sourceSegment.startDepthFeet(), sourceSegment.endDepthFeet(), sourceSegment.orientation())));
        }
        for (AnnotationRecord sourceAnnotation : source.annotations()) {
            SegmentRecord restoredSegment = segments.get(sourceAnnotation.segmentId());
            if (restoredSegment == null) throw new IllegalArgumentException("Project archive has an annotation with no segment");
            annotate(restored.id(), restoredSegment.id(), new CreateAnnotationRequest(sourceAnnotation.label(), sourceAnnotation.reviewState()));
        }
        return workspace(restored.id());
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

    private static byte[] readArchiveEntry(ZipInputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > limit) throw new IllegalArgumentException("Project archive entry is too large");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private record InMemoryUpload(String originalName, byte[] contents) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return originalName; }
        @Override public String getContentType() { return null; }
        @Override public boolean isEmpty() { return contents.length == 0; }
        @Override public long getSize() { return contents.length; }
        @Override public byte[] getBytes() { return contents.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(contents); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), contents); }
    }
}
