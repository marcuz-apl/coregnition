package io.github.marcuzapl.coregnition.backend.workflow;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@CrossOrigin(origins = {"http://localhost:3040", "http://127.0.0.1:3040"})
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) { this.service = service; }

    @PostMapping
    ProjectRecord create(@RequestBody CreateProjectRequest request) throws Exception { return service.createProject(request == null ? null : request.name()); }

    @GetMapping
    List<ProjectRecord> projects() { return service.listProjects(); }

    @GetMapping("/{projectId}")
    ProjectWorkspace workspace(@PathVariable("projectId") String projectId) { return service.workspace(projectId); }

    @PostMapping("/{projectId}/assets")
    AssetRecord importAsset(@PathVariable("projectId") String projectId, @RequestPart("file") MultipartFile file) throws Exception { return service.importAsset(projectId, file); }

    @GetMapping("/{projectId}/assets/{assetId}/content")
    ResponseEntity<Resource> content(@PathVariable("projectId") String projectId, @PathVariable("assetId") String assetId) throws Exception {
        Path path = service.assetPath(projectId, assetId);
        String contentType = Files.probeContentType(path);
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(mediaType).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(path.getFileName().toString()).build().toString()).body(new FileSystemResource(path));
    }

    @PostMapping("/{projectId}/segments")
    SegmentRecord segment(@PathVariable("projectId") String projectId, @RequestBody CreateSegmentRequest request) { return service.createSegment(projectId, request); }

    @PostMapping("/{projectId}/segments/{segmentId}/annotations")
    AnnotationRecord annotation(@PathVariable("projectId") String projectId, @PathVariable("segmentId") String segmentId, @RequestBody CreateAnnotationRequest request) { return service.annotate(projectId, segmentId, request); }

    @GetMapping(value = "/{projectId}/export.csv", produces = "text/csv")
    ResponseEntity<String> export(@PathVariable("projectId") String projectId) { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=coregnition-export.csv").body(service.exportCsv(projectId)); }

    @GetMapping(value = "/{projectId}/archive.zip", produces = "application/zip")
    ResponseEntity<byte[]> archive(@PathVariable("projectId") String projectId) throws Exception { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=coregnition-project.zip").body(service.exportArchive(projectId)); }
}
