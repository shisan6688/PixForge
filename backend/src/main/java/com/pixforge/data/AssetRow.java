package com.pixforge.data;

import java.time.LocalDateTime;

/** 素材表的数据库映射行。 */
public class AssetRow {
    public String id, originalFilename, mimeType, storageKey;
    public long userId, sizeBytes;
    public LocalDateTime createdAt;
}
