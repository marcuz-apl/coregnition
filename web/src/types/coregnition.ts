export interface Project {
  id: string;
  name: string;
  createdAt: string;
}

export interface Asset {
  id: string;
  projectId: string;
  originalName: string;
  relativePath: string;
  sha256: string;
  width: number;
  height: number;
  bitDepth?: number | null;
  colorType?: number | null;
  createdAt: string;
}

export interface Segment {
  id: string;
  projectId: string;
  assetId: string;
  startDepthFeet: number;
  endDepthFeet: number;
  orientation: "TOP_TO_BOTTOM" | "BOTTOM_TO_TOP" | string;
  createdAt: string;
  regionX?: number | null;
  regionY?: number | null;
  regionWidth?: number | null;
  regionHeight?: number | null;
}

export interface Annotation {
  id: string;
  segmentId: string;
  revision: number;
  label: string;
  reviewState: "REVIEWED" | "UNREVIEWED" | string;
  createdAt: string;
}

export type JobStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELLED";

export interface AnalysisJob {
  id: string;
  projectId: string;
  jobType: string;
  status: JobStatus;
  progress: number;
  errorMessage?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

export interface Prediction {
  id: string;
  projectId: string;
  jobId: string;
  segmentId: string;
  suggestedLabel: string;
  confidence: number;
  classScores: Record<string, number> | string;
  isUnknown: boolean;
  modelId: string;
  modelChecksum: string;
  preprocessingVersion: string;
  sourceAssetChecksum: string;
  createdAt: string;
}

export interface ProjectWorkspace {
  project: Project;
  assets: Asset[];
  segments: Segment[];
  annotations: Annotation[];
  predictions?: Prediction[];
}

export interface Region {
  x: number;
  y: number;
  width: number;
  height: number;
}

export type ViewMode = "split" | "log" | "inspector";
export type ThemeMode = "dark" | "light";

export const AAPG_LITHOLOGIES = [
  "limestone",
  "dolostone",
  "carbonaceous shale",
  "sandstone",
  "mixed",
  "unknown",
] as const;

export type AAPGLithology = typeof AAPG_LITHOLOGIES[number];
