package com.pixforge.data;

import java.time.LocalDateTime;

/** 任务阶段表的数据库映射行。 */
public class StepRow {
    public String taskId, stage, status, failureCode, failureMessage;
    public int stepOrder;
    public LocalDateTime startedAt, finishedAt;
}
