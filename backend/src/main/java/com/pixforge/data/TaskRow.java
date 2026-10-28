package com.pixforge.data;

import java.time.LocalDateTime;

/** 制作任务表及模板展示字段的数据库映射行。 */
public class TaskRow {
    public String id, templateId, status, currentStage, resultStorageKey, failureCode, failureMessage;
    public String simulationFailStage, templateTitle, templateCoverUrl;
    public long userId;
    public int simulationDelayMs;
    public LocalDateTime createdAt, startedAt, finishedAt, updatedAt;
}
