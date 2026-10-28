import { templates } from "./demo-data";
import { materializeDemoTask } from "./task-state";
import { Asset, ProductionTask, Stage, STAGES, Template, User } from "./types";

const demo = process.env.NEXT_PUBLIC_USE_DEMO_API === "true";
const TASKS_KEY = "pixforge-demo-tasks";
const USER_KEY = "pixforge-demo-user";

export class ApiError extends Error {
  constructor(message: string, public code = "REQUEST_FAILED") { super(message); }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try { response = await fetch(`/api${path}`, { credentials: "include", cache: "no-store", ...options }); }
  catch { throw new ApiError("连接服务失败，请稍后重试。", "NETWORK_ERROR"); }
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(body?.message || `请求失败（${response.status}）`, body?.code || "REQUEST_FAILED");
  }
  return response.json() as Promise<T>;
}

function currentDemoUser(): User {
  const key = typeof window === "undefined" ? "A" : (localStorage.getItem(USER_KEY) === "B" ? "B" : "A");
  return { id: key === "A" ? "1" : "2", demoKey: key, displayName: key === "A" ? "演示用户 A" : "演示用户 B" };
}

function storedTasks(): ProductionTask[] {
  try { return JSON.parse(localStorage.getItem(TASKS_KEY) || "[]") as ProductionTask[]; }
  catch { return []; }
}

export const api = {
  async getSession(signal?: AbortSignal): Promise<User> {
    return demo ? currentDemoUser() : request<User>("/demo/session", { signal });
  },
  async switchUser(key: "A" | "B"): Promise<User> {
    if (demo) { localStorage.setItem(USER_KEY, key); return currentDemoUser(); }
    return request<User>("/demo/session", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ demoKey: key }) });
  },
  async listTemplates(category?: string, signal?: AbortSignal): Promise<{ categories: string[]; items: Template[] }> {
    if (demo) return { categories: [...new Set(templates.map(t => t.category))], items: category ? templates.filter(t => t.category === category) : templates };
    return request(`/templates${category ? `?category=${encodeURIComponent(category)}` : ""}`, { signal });
  },
  async getTemplate(id: string, signal?: AbortSignal): Promise<Template> {
    if (demo) { const template = templates.find(t => t.id === id); if (!template) throw new ApiError("未找到该模板", "NOT_FOUND"); return template; }
    return request(`/templates/${encodeURIComponent(id)}`, { signal });
  },
  async uploadAsset(file: File, onProgress: (progress: number) => void, signal?: AbortSignal): Promise<Asset> {
    if (demo) {
      onProgress(20);
      await new Promise<void>((resolve, reject) => { const timer = setTimeout(resolve, 550); signal?.addEventListener("abort", () => { clearTimeout(timer); reject(new DOMException("Aborted", "AbortError")); }, { once: true }); });
      onProgress(100);
      return { id: crypto.randomUUID(), previewUrl: "", originalFilename: file.name, mimeType: file.type, sizeBytes: file.size };
    }
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const abort = () => { xhr.abort(); reject(new DOMException("Aborted", "AbortError")); };
      signal?.addEventListener("abort", abort, { once: true });
      xhr.open("POST", "/api/assets");
      xhr.withCredentials = true;
      xhr.upload.onprogress = event => { if (event.lengthComputable) onProgress(Math.round(event.loaded / event.total * 100)); };
      xhr.onload = () => {
        signal?.removeEventListener("abort", abort);
        if (xhr.status >= 200 && xhr.status < 300) resolve(JSON.parse(xhr.responseText));
        else { let body; try { body = JSON.parse(xhr.responseText); } catch { /* empty response */ } reject(new ApiError(body?.message || "图片上传失败，请重试。", body?.code)); }
      };
      xhr.onerror = () => reject(new ApiError("图片上传失败，请检查网络。", "NETWORK_ERROR"));
      const data = new FormData(); data.append("file", file); xhr.send(data);
    });
  },
  async createTask(template: Template, assetIds: string[], simulation: { failAtStage: Stage | null; stepDelayMs: number }): Promise<ProductionTask> {
    if (demo) {
      const user = currentDemoUser();
      const task: ProductionTask & { userId: string } = {
        id: crypto.randomUUID(), userId: user.id, templateId: template.id, templateTitle: template.title, templateCoverUrl: template.coverUrl,
        status: "QUEUED", currentStage: null, steps: STAGES.map(stage => ({ stage, status: "PENDING" })), createdAt: new Date().toISOString(),
        resultUrl: template.resultSampleUrl, failureMessage: null, simulation,
      };
      localStorage.setItem(TASKS_KEY, JSON.stringify([task, ...storedTasks()]));
      return task;
    }
    return request("/tasks", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ templateId: template.id, assetIds, simulation }) });
  },
  async listTasks(signal?: AbortSignal): Promise<ProductionTask[]> {
    if (demo) return storedTasks().filter(task => (task as ProductionTask & { userId: string }).userId === currentDemoUser().id).map(task => materializeDemoTask(task, Date.now()));
    const page = await request<{ items: ProductionTask[] }>("/tasks?page=0&size=100", { signal });
    return page.items;
  },
  async getTask(id: string, signal?: AbortSignal): Promise<ProductionTask> {
    if (demo) {
      const task = storedTasks().find(item => item.id === id && (item as ProductionTask & { userId: string }).userId === currentDemoUser().id);
      if (!task) throw new ApiError("任务不存在", "NOT_FOUND");
      return materializeDemoTask(task, Date.now());
    }
    return request(`/tasks/${encodeURIComponent(id)}`, { signal });
  },
};
