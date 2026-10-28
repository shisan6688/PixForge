package com.pixforge.data;

/** 模板表及扩展素材数量限制的数据库映射行。 */
public class TemplateRow {
    public String id, category, title, description, coverPath, resultSamplePath, acceptedMimeTypes, accent, badge;
    public int requiredImageCount;
    public int maxImageCount;
    public long maxFileSizeBytes;
    public boolean isEnabled;
}
