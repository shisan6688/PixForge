type UploadState = "empty" | "uploading" | "ready" | "error";

export function readyAssetIds(
  slots: ReadonlyArray<{ status: UploadState; asset: { id: string } | null }>,
  minimum: number,
  maximum: number,
): string[] | null {
  if (slots.length < minimum || slots.length > maximum) return null;
  const ids: string[] = [];
  for (const slot of slots) {
    if (slot.status === "empty") continue;
    if (slot.status !== "ready" || !slot.asset) return null;
    ids.push(slot.asset.id);
  }
  return ids.length >= minimum && ids.length <= maximum ? ids : null;
}
