import { describe, expect, it } from "vitest";
import { materializeDemoTask, mergeTasks } from "./task-state";
import { matchesTaskFilter, ProductionTask, STAGES } from "./types";

const makeTask = (id: string, failAtStage: "GENERATE" | null = null): ProductionTask => ({ id, templateId: "a", templateTitle: "A", templateCoverUrl: "/a.jpg", status: "QUEUED", currentStage: null, steps: STAGES.map(stage => ({ stage, status: "PENDING" })), createdAt: "2026-01-01T00:00:00.000Z", resultUrl: "/result.jpg", failureMessage: null, simulation: { failAtStage, stepDelayMs: 1000 } });

describe("task state", () => {
  it("keeps multiple task IDs independent", () => {
    const success = materializeDemoTask(makeTask("one"), Date.parse("2026-01-01T00:00:04.000Z"));
    const failed = materializeDemoTask(makeTask("two", "GENERATE"), Date.parse("2026-01-01T00:00:04.000Z"));
    const merged = mergeTasks([success], [failed]);
    expect(merged).toHaveLength(2);
    expect(merged.find(task => task.id === "one")?.status).toBe("SUCCEEDED");
    expect(merged.find(task => task.id === "two")?.status).toBe("FAILED");
    expect(failed.steps.map(step => step.status)).toEqual(["SUCCEEDED", "FAILED", "PENDING"]);
  });
  it("updates only the matching task when another task changes stage or fails", () => {
    const first = materializeDemoTask(makeTask("one"), Date.parse("2026-01-01T00:00:01.500Z"));
    const second = materializeDemoTask(makeTask("two"), Date.parse("2026-01-01T00:00:01.500Z"));
    const failedUpdate = materializeDemoTask(makeTask("two", "GENERATE"), Date.parse("2026-01-01T00:00:02.500Z"));
    const merged = mergeTasks([first, second], [failedUpdate]);

    expect(merged.find(task => task.id === "one")).toEqual(first);
    expect(merged.find(task => task.id === "two")).toEqual(failedUpdate);
    expect(merged.find(task => task.id === "one")?.status).toBe("RUNNING");
    expect(merged.find(task => task.id === "two")?.status).toBe("FAILED");
  });
  it("does not turn a request error into task failure", () => {
    const running = materializeDemoTask(makeTask("one"), Date.parse("2026-01-01T00:00:01.500Z"));
    expect(running.status).toBe("RUNNING");
    expect(mergeTasks([running], [])[0].status).toBe("RUNNING");
  });
  it("keeps completed and failed tasks in separate views", () => {
    const completed = { ...makeTask("done"), status: "SUCCEEDED" as const };
    const failed = { ...makeTask("failed"), status: "FAILED" as const };
    expect([completed, failed].filter(task => matchesTaskFilter(task, "SUCCEEDED")).map(task => task.id)).toEqual(["done"]);
    expect([completed, failed].filter(task => matchesTaskFilter(task, "FAILED")).map(task => task.id)).toEqual(["failed"]);
  });
});
