package com.pixforge.data;

import java.time.LocalDateTime;

public class AssetRow {
    public String id, originalFilename, mimeType, storageKey;
    public long userId, sizeBytes;
    public LocalDateTime createdAt;
}
