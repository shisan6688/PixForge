import { describe, expect, it } from "vitest";
import { readyAssetIds } from "./template-upload";

const ready = (id: string) => ({ status: "ready" as const, asset: { id } });
const empty = { status: "empty" as const, asset: null };

describe("template upload counts", () => {
  it("accepts one to three images with unused optional slots", () => {
    expect(readyAssetIds([ready("a"), empty, empty], 1, 3)).toEqual(["a"]);
    expect(readyAssetIds([ready("a"), ready("b")], 1, 3)).toEqual(["a", "b"]);
    expect(readyAssetIds([ready("a"), ready("b"), ready("c")], 1, 3)).toEqual(["a", "b", "c"]);
  });

  it("rejects too few, too many, or unfinished uploads", () => {
    expect(readyAssetIds([empty], 1, 3)).toBeNull();
    expect(readyAssetIds([ready("a")], 2, 2)).toBeNull();
    expect(readyAssetIds([ready("a"), { status: "uploading", asset: null }], 1, 3)).toBeNull();
    expect(readyAssetIds([ready("a"), { status: "error", asset: null }], 1, 3)).toBeNull();
    expect(readyAssetIds([ready("a"), ready("b"), ready("c"), ready("d")], 1, 3)).toBeNull();
  });
});
