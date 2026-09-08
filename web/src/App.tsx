import { ChangeEvent, FormEvent, useState } from "react";

const API = "http://127.0.0.1:8787/api/v1";
const labels = ["limestone", "dolostone", "carbonaceous shale", "unknown", "mixed", "unassessable"];

type Project = { id: string; name: string };
type Asset = { id: string; originalName: string; width: number; height: number; sha256: string };
type Segment = { id: string; assetId: string; startDepthFeet: number; endDepthFeet: number; orientation: string };

export function App() {
  const [project, setProject] = useState<Project | null>(null);
  const [projectName, setProjectName] = useState("Core description pilot");
  const [asset, setAsset] = useState<Asset | null>(null);
  const [segment, setSegment] = useState<Segment | null>(null);
  const [start, setStart] = useState("0");
  const [end, setEnd] = useState("1");
  const [orientation, setOrientation] = useState("TOP_TO_BOTTOM");
  const [label, setLabel] = useState(labels[0]);
  const [reviewState, setReviewState] = useState("REVIEWED");
  const [message, setMessage] = useState("Create a project to begin.");

  async function createProject(event: FormEvent) {
    event.preventDefault();
    const response = await fetch(`${API}/projects`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ name: projectName }) });
    if (!response.ok) return setMessage(await errorMessage(response));
    setProject(await response.json());
    setMessage("Project created. Import a core photograph.");
  }

  async function importAsset(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file || !project) return;
    const form = new FormData();
    form.append("file", file);
    const response = await fetch(`${API}/projects/${project.id}/assets`, { method: "POST", body: form });
    if (!response.ok) return setMessage(await errorMessage(response));
    setAsset(await response.json());
    setMessage("Image imported. Set its depth interval.");
  }

  async function createSegment(event: FormEvent) {
    event.preventDefault();
    if (!project || !asset) return;
    const response = await fetch(`${API}/projects/${project.id}/segments`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ assetId: asset.id, startDepthFeet: Number(start), endDepthFeet: Number(end), orientation }) });
    if (!response.ok) return setMessage(await errorMessage(response));
    setSegment(await response.json());
    setMessage("Segment calibrated. Add its reviewed lithology.");
  }

  async function annotate(event: FormEvent) {
    event.preventDefault();
    if (!project || !segment) return;
    const response = await fetch(`${API}/projects/${project.id}/segments/${segment.id}/annotations`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ label, reviewState }) });
    if (!response.ok) return setMessage(await errorMessage(response));
    setMessage("Annotation saved. Export the project description when ready.");
  }

  return <main>
    <header><p className="eyebrow">COREGNITION / M1</p><h1>Core description workspace</h1><p className="lede">Map a core photograph to depth, record the lithology and keep every decision reviewable.</p></header>
    <p className="status" role="status">{message}</p>
    <section className="grid">
      <form className="card" onSubmit={createProject}><span className="step">01</span><h2>Project</h2><label>Project name<input value={projectName} onChange={(event) => setProjectName(event.target.value)} /></label><button type="submit">Create project</button>{project && <p className="success">{project.name}</p>}</form>
      <section className="card"><span className="step">02</span><h2>Core image</h2><label className="upload">Import PNG, JPEG or TIFF<input type="file" accept="image/png,image/jpeg,image/tiff" onChange={importAsset} disabled={!project} /></label>{asset && project && <><img className="preview" src={`${API}/projects/${project.id}/assets/${asset.id}/content`} alt="Imported core" /><p className="meta">{asset.originalName} · {asset.width} × {asset.height}px</p></>}</section>
      <form className="card" onSubmit={createSegment}><span className="step">03</span><h2>Depth calibration</h2><div className="row"><label>Start (ft)<input type="number" min="0" step="0.01" value={start} onChange={(event) => setStart(event.target.value)} /></label><label>End (ft)<input type="number" min="0" step="0.01" value={end} onChange={(event) => setEnd(event.target.value)} /></label></div><label>Orientation<select value={orientation} onChange={(event) => setOrientation(event.target.value)}><option>TOP_TO_BOTTOM</option><option>BOTTOM_TO_TOP</option></select></label><button type="submit" disabled={!asset}>Save segment</button></form>
      <form className="card" onSubmit={annotate}><span className="step">04</span><h2>Manual description</h2><label>Lithology<select value={label} onChange={(event) => setLabel(event.target.value)}>{labels.map((value) => <option key={value}>{value}</option>)}</select></label><label>Review state<select value={reviewState} onChange={(event) => setReviewState(event.target.value)}><option>REVIEWED</option><option>UNREVIEWED</option></select></label><button type="submit" disabled={!segment}>Save annotation</button>{project && <a className="export" href={`${API}/projects/${project.id}/export.csv`}>Download CSV export</a>}</form>
    </section>
  </main>;
}

async function errorMessage(response: Response) { try { return (await response.json()).error ?? `Request failed (${response.status})`; } catch { return `Request failed (${response.status})`; } }
