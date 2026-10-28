import { ProductionTask, STAGES } from "./types";

/** Advance a demo task from its creation time; the API-backed path never uses this. */
export function materializeDemoTask(task: ProductionTask, now: number): ProductionTask {
  if (!task.simulation || task.status === "SUCCEEDED" || task.status === "FAILED") return task;
  const elapsed = now - Date.parse(task.createdAt);
  const delay = task.simulation.stepDelayMs;
  const stageIndex = Math.min(2, Math.floor(Math.max(0, elapsed) / delay));
  const complete = elapsed >= delay * 3;
  const failAt = task.simulation.failAtStage;
  const failIndex = failAt ? STAGES.indexOf(failAt) : -1;
  const failed = failIndex >= 0 && elapsed >= delay * (failIndex + 1);
  const status = failed ? "FAILED" : complete ? "SUCCEEDED" : elapsed < 350 ? "QUEUED" : "RUNNING";
  const currentStage = status === "SUCCEEDED" ? null : STAGES[failed ? failIndex : stageIndex];
  return {
    ...task,
    status,
    currentStage,
    steps: STAGES.map((stage, index) => ({
      stage,
      status: (failed && index === failIndex ? "FAILED" : index < (failed ? failIndex : complete ? 3 : stageIndex) ? "SUCCEEDED" : !failed && !complete && index === stageIndex && status === "RUNNING" ? "RUNNING" : "PENDING") as "FAILED" | "SUCCEEDED" | "RUNNING" | "PENDING",
    })),
    resultUrl: status === "SUCCEEDED" ? task.resultUrl : null,
    failureMessage: failed ? `${{ CHECK_ASSETS: "素材检查未通过", GENERATE: "模拟生成失败", FINALIZE: "结果整理失败" }[STAGES[failIndex]]}，请更换素材后重试。` : null,
  };
}

export function mergeTasks(previous: ProductionTask[], incoming: ProductionTask[]): ProductionTask[] {
  const byId = new Map(previous.map(task => [task.id, task]));
  incoming.forEach(task => byId.set(task.id, task));
  return [...byId.values()].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
}
