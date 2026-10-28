package com.pixforge.data;

import java.time.LocalDateTime;

public class TaskRow {
    public String id, templateId, status, currentStage, resultStorageKey, failureCode, failureMessage;
    public String simulationFailStage, templateTitle, templateCoverUrl;
    public long userId;
    public int simulationDelayMs;
    public LocalDateTime createdAt, startedAt, finishedAt, updatedAt;
}
