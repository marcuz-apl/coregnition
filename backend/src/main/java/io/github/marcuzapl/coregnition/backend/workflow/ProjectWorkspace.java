package io.github.marcuzapl.coregnition.backend.workflow;

import java.util.List;

public record ProjectWorkspace(ProjectRecord project, List<AssetRecord> assets, List<SegmentRecord> segments) {}
