package com.pixforge.service;

import com.pixforge.data.AssetRow;
import com.pixforge.data.TaskMapper;
import com.pixforge.data.TaskRow;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

/** 通过有界线程池依次执行制作任务的三个阶段。 */
@Service
public class TaskProcessor {
    public static final List<String> STAGES = List.of("CHECK_ASSETS", "GENERATE", "FINALIZE");
    private final TaskMapper mapper;
    private final TaskStateService state;
    private final StorageService storage;
    private final ThreadPoolExecutor executor;

    /** 创建有界执行线程池并注入阶段状态服务。 */
    public TaskProcessor(TaskMapper mapper, TaskStateService state, StorageService storage,
                         @Value("${pixforge.worker-count}") int workers,
                         @Value("${pixforge.queue-capacity}") int capacity) {
        this.mapper = mapper; this.state = state; this.storage = storage;
        if (workers < 1 || capacity < 1) throw new IllegalArgumentException("执行器配置必须大于零");
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(capacity), new ThreadPoolExecutor.AbortPolicy());
    }

    /** 启动时清理上次遗留的未结束任务。 */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() { state.interruptOldTasks(); }

    /** 将已提交的任务放入后台执行队列。 */
    public void submit(String id) {
        try { executor.execute(() -> process(id)); }
        catch (RejectedExecutionException exception) {
            state.fail(id, null, "EXECUTOR_OVERLOADED", "任务队列已满，请稍后再试");
        }
    }

    /** 执行素材检查、模拟生成和结果整理，并记录失败。 */
    public void process(String id) {
        TaskRow task = mapper.find(id);
        if (task == null) return;
        String stage = null;
        try {
            for (String next : STAGES) {
                stage = next;
                if (!state.start(id, stage)) return;
                Thread.sleep(task.simulationDelayMs);
                if (stage.equals(task.simulationFailStage))
                    throw new StageFailure("SIMULATED_FAILURE", "模拟在" + stageName(stage) + "阶段失败");
                if ("CHECK_ASSETS".equals(stage)) {
                    List<AssetRow> assets = mapper.assets(id);
                    if (assets.isEmpty() || assets.stream().anyMatch(asset -> !storage.exists(asset.storageKey)))
                        throw new StageFailure("ASSET_MISSING", "素材文件不可用");
                }
                String result = null;
                if ("FINALIZE".equals(stage)) {
                    List<AssetRow> assets = mapper.assets(id);
                    result = "travel-postcard".equals(task.templateId)
                        ? storage.makePostcardResult(id, assets)
                        : storage.makeResult(id, assets.get(0));
                }
                state.finish(id, stage, result);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            state.fail(id, stage, "SERVICE_INTERRUPTED", "服务中断，制作未完成");
        } catch (StageFailure exception) {
            state.fail(id, stage, exception.code, exception.getMessage());
        } catch (Exception exception) {
            state.fail(id, stage, "PROCESSING_ERROR", "处理失败，请重新提交任务");
        }
    }

    /** 将阶段代码转换为可读名称。 */
    private String stageName(String stage) {
        return switch (stage) { case "CHECK_ASSETS" -> "素材检查"; case "GENERATE" -> "模拟生成"; default -> "结果整理"; };
    }

    /** 服务关闭时停止任务执行线程。 */
    @PreDestroy
    public void shutdown() { executor.shutdownNow(); }

    /** 携带业务失败代码的阶段异常。 */
    private static class StageFailure extends Exception {
        final String code;
        StageFailure(String code, String message) { super(message); this.code = code; }
    }
}
