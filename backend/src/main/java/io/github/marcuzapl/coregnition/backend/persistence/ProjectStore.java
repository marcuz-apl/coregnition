package io.github.marcuzapl.coregnition.backend.persistence;

import io.github.marcuzapl.coregnition.backend.workflow.AnnotationRecord;
import io.github.marcuzapl.coregnition.backend.workflow.AssetRecord;
import io.github.marcuzapl.coregnition.backend.workflow.ProjectRecord;
import io.github.marcuzapl.coregnition.backend.workflow.SegmentRecord;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectStore {
    private final JdbcTemplate jdbc;

    public ProjectStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        initialize();
    }

    private void initialize() {
        jdbc.execute("PRAGMA foreign_keys = ON");
        jdbc.execute("CREATE TABLE IF NOT EXISTS projects (id TEXT PRIMARY KEY, name TEXT NOT NULL, created_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS assets (id TEXT PRIMARY KEY, project_id TEXT NOT NULL REFERENCES projects(id), original_name TEXT NOT NULL, relative_path TEXT NOT NULL, sha256 TEXT NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, bit_depth INTEGER, color_type INTEGER, created_at TEXT NOT NULL, UNIQUE(project_id, sha256))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS segments (id TEXT PRIMARY KEY, project_id TEXT NOT NULL REFERENCES projects(id), asset_id TEXT NOT NULL REFERENCES assets(id), start_depth_feet REAL NOT NULL, end_depth_feet REAL NOT NULL, orientation TEXT NOT NULL, region_x INTEGER, region_y INTEGER, region_width INTEGER, region_height INTEGER, created_at TEXT NOT NULL, CHECK(start_depth_feet < end_depth_feet))");
        for (String column : List.of("region_x", "region_y", "region_width", "region_height")) { try { jdbc.execute("ALTER TABLE segments ADD COLUMN " + column + " INTEGER"); } catch (RuntimeException ignored) { } }
        jdbc.execute("CREATE TABLE IF NOT EXISTS annotations (id TEXT PRIMARY KEY, segment_id TEXT NOT NULL REFERENCES segments(id), revision INTEGER NOT NULL, label TEXT NOT NULL, review_state TEXT NOT NULL, created_at TEXT NOT NULL, UNIQUE(segment_id, revision))");
    }

    public ProjectRecord createProject(String name) {
        String id = UUID.randomUUID().toString();
        String created = Instant.now().toString();
        jdbc.update("INSERT INTO projects(id,name,created_at) VALUES(?,?,?)", id, name, created);
        return new ProjectRecord(id, name, created);
    }

    public Optional<ProjectRecord> project(String id) {
        return jdbc.query("SELECT id,name,created_at FROM projects WHERE id=?", this::projectRow, id).stream().findFirst();
    }

    public List<ProjectRecord> projects() {
        return jdbc.query("SELECT id,name,created_at FROM projects ORDER BY created_at DESC", this::projectRow);
    }

    public AssetRecord createAsset(String projectId, String originalName, String relativePath, String sha256, int width, int height, Integer bitDepth, Integer colorType) {
        String id = UUID.randomUUID().toString();
        String created = Instant.now().toString();
        jdbc.update("INSERT INTO assets(id,project_id,original_name,relative_path,sha256,width,height,bit_depth,color_type,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)", id, projectId, originalName, relativePath, sha256, width, height, bitDepth, colorType, created);
        return new AssetRecord(id, projectId, originalName, relativePath, sha256, width, height, bitDepth, colorType, created);
    }

    public Optional<AssetRecord> asset(String projectId, String id) {
        return jdbc.query("SELECT id,project_id,original_name,relative_path,sha256,width,height,bit_depth,color_type,created_at FROM assets WHERE project_id=? AND id=?", this::assetRow, projectId, id).stream().findFirst();
    }

    public Optional<AssetRecord> assetBySha(String projectId, String sha256) {
        return jdbc.query("SELECT id,project_id,original_name,relative_path,sha256,width,height,bit_depth,color_type,created_at FROM assets WHERE project_id=? AND sha256=?", this::assetRow, projectId, sha256).stream().findFirst();
    }

    public List<AssetRecord> assets(String projectId) {
        return jdbc.query("SELECT id,project_id,original_name,relative_path,sha256,width,height,bit_depth,color_type,created_at FROM assets WHERE project_id=? ORDER BY created_at", this::assetRow, projectId);
    }

    public List<SegmentRecord> segments(String projectId) {
        return jdbc.query("SELECT id,project_id,asset_id,start_depth_feet,end_depth_feet,orientation,created_at,region_x,region_y,region_width,region_height FROM segments WHERE project_id=? ORDER BY start_depth_feet,end_depth_feet", (rs, row) -> segmentRow(rs), projectId);
    }

    public boolean hasOverlappingSegment(String projectId, String assetId, double start, double end) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM segments WHERE project_id=? AND asset_id=? AND start_depth_feet < ? AND end_depth_feet > ?", Integer.class, projectId, assetId, end, start);
        return count != null && count > 0;
    }

    public List<AnnotationRecord> annotations(String projectId) {
        return jdbc.query("SELECT r.id,r.segment_id,r.revision,r.label,r.review_state,r.created_at FROM annotations r JOIN segments s ON s.id=r.segment_id WHERE s.project_id=? AND r.revision=(SELECT MAX(r2.revision) FROM annotations r2 WHERE r2.segment_id=r.segment_id) ORDER BY r.created_at", (rs, row) -> new AnnotationRecord(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6)), projectId);
    }

    public Optional<SegmentRecord> segment(String projectId, String segmentId) {
        return jdbc.query("SELECT id,project_id,asset_id,start_depth_feet,end_depth_feet,orientation,created_at,region_x,region_y,region_width,region_height FROM segments WHERE project_id=? AND id=?", (rs, row) -> segmentRow(rs), projectId, segmentId).stream().findFirst();
    }

    public Optional<AnnotationRecord> undoLatestAnnotation(String segmentId) {
        Optional<AnnotationRecord> latest = jdbc.query("SELECT id,segment_id,revision,label,review_state,created_at FROM annotations WHERE segment_id=? ORDER BY revision DESC LIMIT 1", (rs, row) -> new AnnotationRecord(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6)), segmentId).stream().findFirst();
        latest.ifPresent(annotation -> jdbc.update("DELETE FROM annotations WHERE id=?", annotation.id()));
        return jdbc.query("SELECT id,segment_id,revision,label,review_state,created_at FROM annotations WHERE segment_id=? ORDER BY revision DESC LIMIT 1", (rs, row) -> new AnnotationRecord(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6)), segmentId).stream().findFirst();
    }

    public SegmentRecord createSegment(String projectId, String assetId, double start, double end, String orientation, Integer regionX, Integer regionY, Integer regionWidth, Integer regionHeight) {
        String id = UUID.randomUUID().toString();
        String created = Instant.now().toString();
        jdbc.update("INSERT INTO segments(id,project_id,asset_id,start_depth_feet,end_depth_feet,orientation,region_x,region_y,region_width,region_height,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)", id, projectId, assetId, start, end, orientation, regionX, regionY, regionWidth, regionHeight, created);
        return new SegmentRecord(id, projectId, assetId, start, end, orientation, created, regionX, regionY, regionWidth, regionHeight);
    }

    public AnnotationRecord annotate(String segmentId, String label, String reviewState) {
        Integer previous = jdbc.queryForObject("SELECT MAX(revision) FROM annotations WHERE segment_id=?", Integer.class, segmentId);
        int revision = previous == null ? 1 : previous + 1;
        String id = UUID.randomUUID().toString();
        String created = Instant.now().toString();
        jdbc.update("INSERT INTO annotations(id,segment_id,revision,label,review_state,created_at) VALUES(?,?,?,?,?,?)", id, segmentId, revision, label, reviewState, created);
        return new AnnotationRecord(id, segmentId, revision, label, reviewState, created);
    }

    public List<SegmentExportRow> exportRows(String projectId) {
        return jdbc.query("SELECT s.id,s.asset_id,a.original_name,s.start_depth_feet,s.end_depth_feet,s.orientation,COALESCE(r.label,''),COALESCE(r.review_state,'UNREVIEWED') FROM segments s JOIN assets a ON a.id=s.asset_id LEFT JOIN annotations r ON r.segment_id=s.id AND r.revision=(SELECT MAX(r2.revision) FROM annotations r2 WHERE r2.segment_id=s.id) WHERE s.project_id=? ORDER BY s.start_depth_feet,s.end_depth_feet", (rs, row) -> new SegmentExportRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getDouble(5), rs.getString(6), rs.getString(7), rs.getString(8)), projectId);
    }

    private ProjectRecord projectRow(ResultSet rs, int row) throws java.sql.SQLException { return new ProjectRecord(rs.getString(1), rs.getString(2), rs.getString(3)); }
    private AssetRecord assetRow(ResultSet rs, int row) throws java.sql.SQLException { return new AssetRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6), rs.getInt(7), (Integer) rs.getObject(8), (Integer) rs.getObject(9), rs.getString(10)); }
    private SegmentRecord segmentRow(ResultSet rs) throws java.sql.SQLException { return new SegmentRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getDouble(5), rs.getString(6), rs.getString(7), (Integer) rs.getObject(8), (Integer) rs.getObject(9), (Integer) rs.getObject(10), (Integer) rs.getObject(11)); }

    public record SegmentExportRow(String id, String assetId, String originalName, double startFeet, double endFeet, String orientation, String label, String reviewState) {}
}
