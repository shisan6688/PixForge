package com.pixforge.data;

import java.time.LocalDateTime;

public class StepRow {
    public String taskId, stage, status, failureCode, failureMessage;
    public int stepOrder;
    public LocalDateTime startedAt, finishedAt;
}
