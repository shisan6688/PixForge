package com.pixforge.service;

import com.pixforge.api.ResourceUnavailableException;
import com.pixforge.api.TemplateController;
import com.pixforge.data.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TaskService {
    public record Simulation(String failAtStage, Integer stepDelayMs) {}
    public record CreateRequest(String templateId, List<String> assetIds, Simulation simulation) {}
    public record StepView(String stage, String status, String failureCode, String failureMessage) {}
    public record TaskView(String id, String templateId, String templateTitle, String templateCoverUrl,
                           String status, String currentStage, List<StepView> steps, String createdAt,
                           String resultUrl, String failureCode, String failureMessage) {}
    public record TaskPage(List<TaskView> items, long total, int page, int size) {}

    private final TemplateMapper templates;
    private final AssetMapper assets;
    private final TaskMapper tasks;
    private final StorageService storage;
    private final TaskProcessor processor;
    private final boolean demoControlsEnabled;

    public TaskService(TemplateMapper templates, AssetMapper assets, TaskMapper tasks, StorageService storage,
                       TaskProcessor processor, @Value("${pixforge.demo-controls-enabled}") boolean demoControlsEnabled) {
        this.templates = templates; this.assets = assets; this.tasks = tasks;
        this.storage = storage; this.processor = processor; this.demoControlsEnabled = demoControlsEnabled;
    }

    @Transactional
    public TaskView create(CreateRequest request, long userId) {
        if (request == null || request.templateId() == null || request.templateId().isBlank())
            throw new IllegalArgumentException("请选择模板");
        TemplateRow template = templates.find(request.templateId());
        if (template == null) throw new ResourceUnavailableException("未找到该模板");
        List<String> ids = request.assetIds();
        if (ids == null || ids.size() < template.requiredImageCount || ids.size() > template.maxImageCount
            || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("素材数量或上传位不正确");
        for (String id : ids) {
            AssetRow asset = assets.owned(id, userId);
            if (asset == null || !storage.exists(asset.storageKey)) throw new IllegalArgumentException("素材不存在或不属于当前用户");
            if (!TemplateController.view(template).acceptedMimeTypes().contains(asset.mimeType) || asset.sizeBytes > template.maxFileSizeBytes)
                throw new IllegalArgumentException("素材格式或大小不符合模板要求");
        }
        Simulation simulation = request.simulation();
        if (!demoControlsEnabled && simulation != null) throw new IllegalArgumentException("当前环境不接受模拟参数");
        String failStage = simulation == null ? null : simulation.failAtStage();
        int delay = simulation == null || simulation.stepDelayMs() == null ? 2000 : simulation.stepDelayMs();
        if (failStage != null && !TaskProcessor.STAGES.contains(failStage)) throw new IllegalArgumentException("无效的失败阶段");
        if (delay < 0 || delay > 10000) throw new IllegalArgumentException("每阶段耗时须在 0 到 10000 毫秒之间");

        TaskRow row = new TaskRow();
        row.id = UUID.randomUUID().toString(); row.userId = userId; row.templateId = template.id;
        row.status = "QUEUED"; row.simulationFailStage = failStage; row.simulationDelayMs = delay;
        row.createdAt = LocalDateTime.now(ZoneOffset.UTC); row.updatedAt = row.createdAt;
        tasks.insert(row);
        for (int index = 0; index < ids.size(); index++) tasks.insertAsset(row.id, index, ids.get(index));
        for (int index = 0; index < TaskProcessor.STAGES.size(); index++) tasks.insertStep(row.id, TaskProcessor.STAGES.get(index), index);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { processor.submit(row.id); }
        });
        return new TaskView(row.id, row.templateId, template.title, template.coverPath, "QUEUED", null,
            TaskProcessor.STAGES.stream().map(stage -> new StepView(stage, "PENDING", null, null)).toList(),
            row.createdAt.atOffset(ZoneOffset.UTC).toString(), null, null, null);
    }

    public TaskView get(String id, long userId) {
        TaskRow row = tasks.owned(id, userId);
        if (row == null) throw new ResourceUnavailableException("任务不可用");
        return view(row);
    }

    public TaskPage list(long userId, int page, int size, boolean active) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("分页参数无效");
        int offset;
        try { offset = Math.multiplyExact(page, size); }
        catch (ArithmeticException exception) { throw new IllegalArgumentException("分页参数过大"); }
        int flag = active ? 1 : 0;
        return new TaskPage(tasks.list(userId, flag, size, offset).stream().map(this::view).toList(),
            tasks.count(userId, flag), page, size);
    }

    private TaskView view(TaskRow row) {
        return new TaskView(row.id, row.templateId, row.templateTitle, row.templateCoverUrl,
            row.status, row.currentStage,
            tasks.steps(row.id).stream().map(step -> new StepView(step.stage, step.status, step.failureCode, step.failureMessage)).toList(),
            row.createdAt.atOffset(ZoneOffset.UTC).toString(),
            "SUCCEEDED".equals(row.status) ? "/api/tasks/" + row.id + "/result" : null,
            row.failureCode, row.failureMessage);
    }
}
