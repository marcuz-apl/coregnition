package io.github.marcuzapl.coregnition.backend.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectWorkspace(
    ProjectRecord project,
    List<AssetRecord> assets,
    List<SegmentRecord> segments,
    List<AnnotationRecord> annotations,
    List<PredictionRecord> predictions
) {
    public ProjectWorkspace(ProjectRecord project, List<AssetRecord> assets, List<SegmentRecord> segments, List<AnnotationRecord> annotations) {
        this(project, assets, segments, annotations, List.of());
    }
}
