import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
import {
  Project,
  ProjectWorkspace,
  Asset,
  Segment,
  Annotation,
  Region,
  ViewMode,
  ThemeMode,
  AnalysisJob,
  Prediction,
} from "../types/coregnition";
import { api } from "../api/client";

interface WorkspaceContextValue {
  projects: Project[];
  activeProject: Project | null;
  assets: Asset[];
  segments: Segment[];
  annotations: Annotation[];
  activeAsset: Asset | null;
  activeSegment: Segment | null;
  activeRegion: Region | null;
  theme: ThemeMode;
  viewMode: ViewMode;
  isLeftDockOpen: boolean;
  isRightDockOpen: boolean;
  zoom: number;
  rotation: number;
  brightness: number;
  contrast: number;
  isLoading: boolean;
  error: string | null;

  // M2: Analysis & Predictions
  activeJob: AnalysisJob | null;
  predictions: Prediction[];
  runAnalysis: (segmentId?: string) => Promise<void>;
  cancelAnalysis: () => Promise<void>;
  acceptPrediction: (segmentId: string, predictionId?: string) => Promise<void>;

  loadProjects: () => Promise<void>;
  selectProject: (id: string) => Promise<void>;
  createProject: (name: string) => Promise<Project>;
  importAsset: (file: File) => Promise<Asset>;
  createSegment: (
    assetId: string,
    startDepth: number,
    endDepth: number,
    orientation: string,
    region?: Region | null
  ) => Promise<Segment>;
  createAnnotation: (
    segmentId: string,
    label: string,
    reviewState: string
  ) => Promise<Annotation>;
  undoAnnotation: (segmentId: string) => Promise<void>;
  importArchive: (file: File) => Promise<void>;

  setActiveAsset: (asset: Asset | null) => void;
  setActiveSegment: (segment: Segment | null) => void;
  setActiveRegion: (region: Region | null) => void;
  setTheme: (theme: ThemeMode) => void;
  toggleTheme: () => void;
  setViewMode: (mode: ViewMode) => void;
  toggleLeftDock: () => void;
  toggleRightDock: () => void;
  setZoom: React.Dispatch<React.SetStateAction<number>>;
  setRotation: React.Dispatch<React.SetStateAction<number>>;
  setBrightness: React.Dispatch<React.SetStateAction<number>>;
  setContrast: React.Dispatch<React.SetStateAction<number>>;
  clearError: () => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | undefined>(undefined);

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [activeProject, setActiveProject] = useState<Project | null>(null);
  const [assets, setAssets] = useState<Asset[]>([]);
  const [segments, setSegments] = useState<Segment[]>([]);
  const [annotations, setAnnotations] = useState<Annotation[]>([]);
  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [activeJob, setActiveJob] = useState<AnalysisJob | null>(null);

  const [activeAsset, setActiveAsset] = useState<Asset | null>(null);
  const [activeSegment, setActiveSegment] = useState<Segment | null>(null);
  const [activeRegion, setActiveRegion] = useState<Region | null>(null);

  const [theme, setTheme] = useState<ThemeMode>("dark");
  const [viewMode, setViewMode] = useState<ViewMode>("split");
  const [isLeftDockOpen, setIsLeftDockOpen] = useState(true);
  const [isRightDockOpen, setIsRightDockOpen] = useState(true);

  const [zoom, setZoom] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [brightness, setBrightness] = useState(100);
  const [contrast, setContrast] = useState(100);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  const selectProject = useCallback(async (id: string) => {
    try {
      setIsLoading(true);
      const ws = await api.getWorkspace(id);
      setActiveProject(ws.project);
      setAssets(ws.assets);
      setSegments(ws.segments);
      setAnnotations(ws.annotations);
      if (ws.predictions) {
        setPredictions(ws.predictions);
      } else {
        const preds = await api.listPredictions(id);
        setPredictions(preds);
      }

      if (ws.assets.length > 0) {
        setActiveAsset(ws.assets[0]);
      } else {
        setActiveAsset(null);
      }
      setActiveSegment(null);
      setActiveRegion(null);
      setActiveJob(null);
    } catch (err: any) {
      setError(err.message || "Failed to load project workspace");
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadProjects = useCallback(async () => {
    try {
      setIsLoading(true);
      const list = await api.listProjects();
      setProjects(list);
      if (list.length > 0 && !activeProject) {
        await selectProject(list[0].id);
      }
    } catch (err: any) {
      setError(err.message || "Failed to load projects");
    } finally {
      setIsLoading(false);
    }
  }, [activeProject, selectProject]);

  // Active Job Polling
  useEffect(() => {
    if (!activeJob || !activeProject) return;
    if (activeJob.status === "SUCCEEDED" || activeJob.status === "FAILED" || activeJob.status === "CANCELLED") {
      return;
    }

    const interval = setInterval(async () => {
      try {
        const current = await api.getJob(activeProject.id, activeJob.id);
        setActiveJob(current);
        if (current.status === "SUCCEEDED") {
          const preds = await api.listPredictions(activeProject.id);
          setPredictions(preds);
        }
      } catch {
        // ignore transient network glitch
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [activeJob, activeProject]);

  const runAnalysis = useCallback(
    async (segmentId?: string) => {
      if (!activeProject) return;
      try {
        setError(null);
        const job = await api.startAnalysis(activeProject.id, segmentId);
        setActiveJob(job);
      } catch (err: any) {
        setError(err.message || "Failed to start analysis job");
      }
    },
    [activeProject]
  );

  const cancelAnalysis = useCallback(async () => {
    if (!activeProject || !activeJob) return;
    try {
      await api.cancelJob(activeProject.id, activeJob.id);
      setActiveJob((prev) => (prev ? { ...prev, status: "CANCELLED" } : null));
    } catch (err: any) {
      setError(err.message || "Failed to cancel analysis job");
    }
  }, [activeProject, activeJob]);

  const acceptPrediction = useCallback(
    async (segmentId: string, predictionId?: string) => {
      if (!activeProject) return;
      try {
        setIsLoading(true);
        const annotation = await api.acceptPrediction(activeProject.id, segmentId, predictionId);
        setAnnotations((prev) => [
          ...prev.filter((a) => a.segmentId !== segmentId),
          annotation,
        ]);
      } catch (err: any) {
        setError(err.message || "Failed to accept prediction");
      } finally {
        setIsLoading(false);
      }
    },
    [activeProject]
  );

  const createProject = useCallback(
    async (name: string): Promise<Project> => {
      try {
        setIsLoading(true);
        const proj = await api.createProject(name);
        setProjects((prev) => [...prev, proj]);
        await selectProject(proj.id);
        return proj;
      } catch (err: any) {
        setError(err.message || "Failed to create project");
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [selectProject]
  );

  const importAsset = useCallback(
    async (file: File): Promise<Asset> => {
      if (!activeProject) throw new Error("No active project selected");
      try {
        setIsLoading(true);
        const asset = await api.importAsset(activeProject.id, file);
        setAssets((prev) => [...prev, asset]);
        setActiveAsset(asset);
        return asset;
      } catch (err: any) {
        setError(err.message || "Failed to import asset");
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [activeProject]
  );

  const createSegment = useCallback(
    async (
      assetId: string,
      startDepth: number,
      endDepth: number,
      orientation: string,
      region?: Region | null
    ): Promise<Segment> => {
      if (!activeProject) throw new Error("No active project selected");
      try {
        setIsLoading(true);
        const seg = await api.createSegment(activeProject.id, {
          assetId,
          startDepthFeet: startDepth,
          endDepthFeet: endDepth,
          orientation,
          region,
        });
        setSegments((prev) => [...prev, seg]);
        setActiveSegment(seg);
        return seg;
      } catch (err: any) {
        setError(err.message || "Failed to create segment");
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [activeProject]
  );

  const createAnnotation = useCallback(
    async (
      segmentId: string,
      label: string,
      reviewState: string
    ): Promise<Annotation> => {
      if (!activeProject) throw new Error("No active project selected");
      try {
        setIsLoading(true);
        const ann = await api.createAnnotation(activeProject.id, segmentId, {
          label,
          reviewState,
        });
        setAnnotations((prev) => [
          ...prev.filter((a) => a.segmentId !== segmentId),
          ann,
        ]);
        return ann;
      } catch (err: any) {
        setError(err.message || "Failed to record annotation");
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [activeProject]
  );

  const undoAnnotation = useCallback(
    async (segmentId: string) => {
      if (!activeProject) return;
      try {
        setIsLoading(true);
        const prior = await api.undoAnnotation(activeProject.id, segmentId);
        setAnnotations((prev) => {
          const filtered = prev.filter((a) => a.segmentId !== segmentId);
          return prior ? [...filtered, prior] : filtered;
        });
      } catch (err: any) {
        setError(err.message || "Failed to undo annotation");
      } finally {
        setIsLoading(false);
      }
    },
    [activeProject]
  );

  const importArchive = useCallback(
    async (file: File) => {
      try {
        setIsLoading(true);
        const ws = await api.importArchive(file);
        setProjects((prev) => {
          if (prev.some((p) => p.id === ws.project.id)) return prev;
          return [...prev, ws.project];
        });
        setActiveProject(ws.project);
        setAssets(ws.assets);
        setSegments(ws.segments);
        setAnnotations(ws.annotations);
        if (ws.predictions) setPredictions(ws.predictions);
        if (ws.assets.length > 0) setActiveAsset(ws.assets[0]);
      } catch (err: any) {
        setError(err.message || "Failed to import archive");
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const toggleTheme = useCallback(() => {
    setTheme((prev) => (prev === "dark" ? "light" : "dark"));
  }, []);

  const toggleLeftDock = useCallback(() => {
    setIsLeftDockOpen((prev) => !prev);
  }, []);

  const toggleRightDock = useCallback(() => {
    setIsRightDockOpen((prev) => !prev);
  }, []);

  const clearError = useCallback(() => setError(null), []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  return (
    <WorkspaceContext.Provider
      value={{
        projects,
        activeProject,
        assets,
        segments,
        annotations,
        activeAsset,
        activeSegment,
        activeRegion,
        theme,
        viewMode,
        isLeftDockOpen,
        isRightDockOpen,
        zoom,
        rotation,
        brightness,
        contrast,
        isLoading,
        error,

        activeJob,
        predictions,
        runAnalysis,
        cancelAnalysis,
        acceptPrediction,

        loadProjects,
        selectProject,
        createProject,
        importAsset,
        createSegment,
        createAnnotation,
        undoAnnotation,
        importArchive,

        setActiveAsset,
        setActiveSegment,
        setActiveRegion,
        setTheme,
        toggleTheme,
        setViewMode,
        toggleLeftDock,
        toggleRightDock,
        setZoom,
        setRotation,
        setBrightness,
        setContrast,
        clearError,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) {
    throw new Error("useWorkspace must be used within a WorkspaceProvider");
  }
  return context;
}
