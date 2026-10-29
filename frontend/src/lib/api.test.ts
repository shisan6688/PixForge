import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "./api";

afterEach(() => vi.unstubAllGlobals());

describe("task query errors", () => {
  it("reports a network query failure separately from a task failure", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("network unavailable")));

    await expect(api.listTasks()).rejects.toMatchObject({
      code: "NETWORK_ERROR",
      message: "连接服务失败，请稍后重试。",
    } satisfies Partial<ApiError>);
    expect(fetch).toHaveBeenCalledWith("/api/tasks?page=0&size=100", expect.objectContaining({ credentials: "include" }));
  });

  it("preserves a server query error code instead of returning a failed task", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: "DB_UNAVAILABLE", message: "查询暂时不可用" }), {
      status: 503,
      headers: { "Content-Type": "application/json" },
    })));

    await expect(api.listTasks("ACTIVE")).rejects.toMatchObject({
      code: "DB_UNAVAILABLE",
      message: "查询暂时不可用",
    } satisfies Partial<ApiError>);
  });
});
