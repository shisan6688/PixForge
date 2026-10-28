export type Stage = "CHECK_ASSETS" | "GENERATE" | "FINALIZE";
export type TaskStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED";
export type TaskFilter = "ALL" | "ACTIVE" | "SUCCEEDED" | "FAILED";
export type TaskQueryStatus = Exclude<TaskFilter, "ALL">;
export type StepStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";

export interface Template {
  id: string;
  title: string;
  category: string;
  description: string;
  coverUrl: string;
  resultSampleUrl: string;
  requiredImageCount: number;
  maxImageCount?: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
  accent: string;
  badge?: string;
}

export interface User { id: string; demoKey: "A" | "B"; displayName: string }
export interface Asset { id: string; previewUrl: string; originalFilename: string; mimeType: string; sizeBytes: number }
export interface TaskStep { stage: Stage; status: StepStatus }
export interface ProductionTask {
  id: string;
  templateId: string;
  templateTitle: string;
  templateCoverUrl: string;
  status: TaskStatus;
  currentStage: Stage | null;
  steps: TaskStep[];
  createdAt: string;
  resultUrl: string | null;
  failureMessage: string | null;
  simulation?: { failAtStage: Stage | null; stepDelayMs: number };
}
export interface TaskCounts { all: number; active: number; succeeded: number; failed: number }
export interface TaskPage { items: ProductionTask[]; total: number; page: number; size: number; counts: TaskCounts }

export const STAGES: Stage[] = ["CHECK_ASSETS", "GENERATE", "FINALIZE"];
export const STAGE_LABELS: Record<Stage, string> = { CHECK_ASSETS: "素材检查", GENERATE: "模拟生成", FINALIZE: "结果整理" };
export const isActive = (task: ProductionTask) => task.status === "QUEUED" || task.status === "RUNNING";
export const matchesTaskFilter = (task: ProductionTask, filter: TaskFilter) =>
  filter === "ALL" || (filter === "ACTIVE" ? isActive(task) : task.status === filter);
