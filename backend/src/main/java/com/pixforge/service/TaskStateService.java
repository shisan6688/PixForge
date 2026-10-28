package com.pixforge.service;

import com.pixforge.data.TaskMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 以独立事务维护任务与阶段的状态转换。 */
@Service
public class TaskStateService {
    private final TaskMapper mapper;
    /** 注入任务与阶段状态的数据库映射。 */
    public TaskStateService(TaskMapper mapper) { this.mapper = mapper; }
    /** 获取当前 UTC 数据库时间。 */
    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }

    /** 启动指定阶段，仅允许未结束任务转换状态。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean start(String id, String stage) {
        LocalDateTime time = now();
        if (mapper.start(id, stage, time) == 0) return false;
        if (mapper.startStep(id, stage, time) != 1) throw new IllegalStateException("阶段状态异常");
        return true;
    }

    /** 完成阶段并在最后一阶段记录结果。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(String id, String stage, String resultKey) {
        LocalDateTime time = now();
        if (mapper.finishStep(id, stage, time) != 1) throw new IllegalStateException("阶段未在运行");
        if (resultKey != null && mapper.succeed(id, resultKey, time) != 1) throw new IllegalStateException("任务无法完成");
    }

    /** 保存阶段和任务的失败代码及原因。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String id, String stage, String code, String message) {
        LocalDateTime time = now();
        if (stage != null) mapper.failStep(id, stage, time, code, message);
        mapper.fail(id, time, code, message);
    }

    /** 将服务重启前遗留的运行任务标记为失败。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void interruptOldTasks() {
        LocalDateTime time = now();
        mapper.interruptSteps(time);
        mapper.interruptTasks(time);
    }
}
