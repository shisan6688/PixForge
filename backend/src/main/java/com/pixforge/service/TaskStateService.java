package com.pixforge.service;

import com.pixforge.data.TaskMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskStateService {
    private final TaskMapper mapper;
    public TaskStateService(TaskMapper mapper) { this.mapper = mapper; }
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean start(String id, String stage) {
        LocalDateTime time = now();
        if (mapper.start(id, stage, time) == 0) return false;
        if (mapper.startStep(id, stage, time) != 1) throw new IllegalStateException("阶段状态异常");
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(String id, String stage, String resultKey) {
        LocalDateTime time = now();
        if (mapper.finishStep(id, stage, time) != 1) throw new IllegalStateException("阶段未在运行");
        if (resultKey != null && mapper.succeed(id, resultKey, time) != 1) throw new IllegalStateException("任务无法完成");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String id, String stage, String code, String message) {
        LocalDateTime time = now();
        if (stage != null) mapper.failStep(id, stage, time, code, message);
        mapper.fail(id, time, code, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void interruptOldTasks() {
        LocalDateTime time = now();
        mapper.interruptSteps(time);
        mapper.interruptTasks(time);
    }
}
