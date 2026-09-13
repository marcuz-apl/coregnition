import { Project, ProjectWorkspace, Asset, Segment, Annotation, Region } from "../types/coregnition";

const API_BASE = "http://localhost:3041/api/v1/projects";

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const err = await res.json();
      message = err.error || err.message || message;
    } catch {
      // ignore
    }
    throw new Error(message);
  }
  return res.json();
}

export const api = {
  async listProjects(): Promise<Project[]> {
    const res = await fetch(API_BASE);
    return handleResponse<Project[]>(res);
  },

  async createProject(name: string): Promise<Project> {
    const res = await fetch(API_BASE, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name }),
    });
    return handleResponse<Project>(res);
  },

  async getWorkspace(projectId: string): Promise<ProjectWorkspace> {
    const res = await fetch(`${API_BASE}/${projectId}`);
    return handleResponse<ProjectWorkspace>(res);
  },

  async importAsset(projectId: string, file: File): Promise<Asset> {
    const form = new FormData();
    form.append("file", file);
    const res = await fetch(`${API_BASE}/${projectId}/assets`, {
      method: "POST",
      body: form,
    });
    return handleResponse<Asset>(res);
  },

  getAssetContentUrl(projectId: string, assetId: string): string {
    return `${API_BASE}/${projectId}/assets/${assetId}/content`;
  },

  async createSegment(
    projectId: string,
    req: {
      assetId: string;
      startDepthFeet: number;
      endDepthFeet: number;
      orientation: string;
      region?: Region | null;
    }
  ): Promise<Segment> {
    const body: Record<string, unknown> = {
      assetId: req.assetId,
      startDepthFeet: req.startDepthFeet,
      endDepthFeet: req.endDepthFeet,
      orientation: req.orientation,
    };
    if (req.region) {
      body.regionX = req.region.x;
      body.regionY = req.region.y;
      body.regionWidth = req.region.width;
      body.regionHeight = req.region.height;
    }
    const res = await fetch(`${API_BASE}/${projectId}/segments`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    return handleResponse<Segment>(res);
  },

  async createAnnotation(
    projectId: string,
    segmentId: string,
    req: { label: string; reviewState: string }
  ): Promise<Annotation> {
    const res = await fetch(`${API_BASE}/${projectId}/segments/${segmentId}/annotations`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req),
    });
    return handleResponse<Annotation>(res);
  },

  async undoAnnotation(projectId: string, segmentId: string): Promise<Annotation | null> {
    const res = await fetch(`${API_BASE}/${projectId}/segments/${segmentId}/annotations/latest`, {
      method: "DELETE",
    });
    if (res.status === 204) return null;
    return handleResponse<Annotation>(res);
  },

  async importArchive(file: File): Promise<ProjectWorkspace> {
    const form = new FormData();
    form.append("file", file);
    const res = await fetch(`${API_BASE}/archive`, {
      method: "POST",
      body: form,
    });
    return handleResponse<ProjectWorkspace>(res);
  },

  getExportCsvUrl(projectId: string, reviewedOnly: boolean = false): string {
    return `${API_BASE}/${projectId}/export.csv?reviewedOnly=${reviewedOnly}`;
  },

  getExportArchiveUrl(projectId: string): string {
    return `${API_BASE}/${projectId}/archive.zip`;
  },
};
