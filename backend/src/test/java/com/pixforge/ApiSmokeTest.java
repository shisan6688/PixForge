package com.pixforge;

import static org.junit.jupiter.api.Assertions.*;

import com.pixforge.api.ResourceUnavailableException;
import com.pixforge.data.*;
import com.pixforge.service.*;
import java.io.IOException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 验证任务执行、素材拼图、访问隔离及状态查询行为。 */
class ApiSmokeTest {
    /** 创建用于单元测试的 Mapper 代理。 */
    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    /** 创建测试任务数据。 */
    private TaskRow task(String failStage) {
        TaskRow row = new TaskRow();
        row.id = "task-1"; row.status = "QUEUED"; row.simulationDelayMs = 0;
        row.simulationFailStage = failStage;
        return row;
    }

    /** 创建返回指定任务和素材的 Mapper。 */
    private TaskMapper mapper(TaskRow task, AssetRow asset) {
        return proxy(TaskMapper.class, (target, method, args) -> switch (method.getName()) {
            case "find" -> task;
            case "assets" -> List.of(asset);
            case "owned" -> null;
            default -> throw new AssertionError("Unexpected mapper call: " + method.getName());
        });
    }

    /** 记录执行器的状态变化事件。 */
    static class RecordingState extends TaskStateService {
        final List<String> events = new ArrayList<>();
        RecordingState(TaskMapper mapper) { super(mapper); }
        @Override public boolean start(String id, String stage) { events.add("start:" + stage); return true; }
        @Override public void finish(String id, String stage, String result) { events.add("finish:" + stage + ":" + result); }
        @Override public void fail(String id, String stage, String code, String message) { events.add("fail:" + stage + ":" + code); }
    }

    /** 为执行器测试提供虚拟文件存储。 */
    static class FakeStorage extends StorageService {
        boolean copied;
        FakeStorage() { super("data"); }
        @Override public boolean exists(String key) { return true; }
        @Override public String makeResult(String id, AssetRow asset) throws IOException { copied = true; return "results/task-1.jpg"; }
    }

    /** 成功任务按顺序执行所有阶段。 */
    @Test void processorRunsStagesInOrderAndCreatesResult() {
        AssetRow asset = new AssetRow(); asset.storageKey = "assets/a.jpg";
        TaskMapper mapper = mapper(task(null), asset);
        RecordingState state = new RecordingState(mapper);
        FakeStorage storage = new FakeStorage();
        TaskProcessor processor = new TaskProcessor(mapper, state, storage, 1, 1);
        try {
            processor.process("task-1");
            assertEquals(List.of(
                "start:CHECK_ASSETS", "finish:CHECK_ASSETS:null",
                "start:GENERATE", "finish:GENERATE:null",
                "start:FINALIZE", "finish:FINALIZE:results/task-1.jpg"), state.events);
            assertTrue(storage.copied);
        } finally { processor.shutdown(); }
    }

    /** 任一指定阶段失败后都不进入后续阶段，也不产生结果。 */
    @Test void simulatedFailureStopsLaterStages() {
        for (int failedIndex = 0; failedIndex < TaskProcessor.STAGES.size(); failedIndex++) {
            String failedStage = TaskProcessor.STAGES.get(failedIndex);
            AssetRow asset = new AssetRow(); asset.storageKey = "assets/a.jpg";
            TaskMapper mapper = mapper(task(failedStage), asset);
            RecordingState state = new RecordingState(mapper);
            FakeStorage storage = new FakeStorage();
            TaskProcessor processor = new TaskProcessor(mapper, state, storage, 1, 1);
            try {
                processor.process("task-1");
                List<String> expected = new ArrayList<>();
                for (int index = 0; index < failedIndex; index++) {
                    String stage = TaskProcessor.STAGES.get(index);
                    expected.add("start:" + stage);
                    expected.add("finish:" + stage + ":null");
                }
                expected.add("start:" + failedStage);
                expected.add("fail:" + failedStage + ":SIMULATED_FAILURE");
                assertEquals(expected, state.events, failedStage);
                assertFalse(storage.copied, failedStage);
            } finally { processor.shutdown(); }
        }
    }

    /** 已结束任务不能再次进入执行状态。 */
    @Test void terminalTaskIsNeverRestarted() {
        TaskMapper mapper = proxy(TaskMapper.class, (target, method, args) -> {
            if (method.getName().equals("start")) return 0;
            throw new AssertionError("Terminal task should not transition");
        });
        assertFalse(new TaskStateService(mapper).start("task-1", "CHECK_ASSETS"));
    }

    /** 用户不能读取其他用户的任务。 */
    @Test void userCannotReadAnotherUsersTask() {
        TaskMapper mapper = mapper(task(null), new AssetRow());
        TaskService service = new TaskService(null, null, mapper, null, null, true);
        assertThrows(ResourceUnavailableException.class, () -> service.get("private-task", 2L));
    }

    /** 拼图结果包含一至三张输入照片。 */
    @Test void postcardResultUsesAllSelectedPhotos(@TempDir Path directory) throws IOException {
        StorageService storage = new StorageService(directory.toString());
        List<AssetRow> photos = new ArrayList<>();
        Color[] colors = { Color.RED, Color.GREEN, Color.BLUE };
        for (int index = 0; index < colors.length; index++) {
            BufferedImage photo = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            var graphics = photo.createGraphics();
            graphics.setColor(colors[index]); graphics.fillRect(0, 0, 200, 200); graphics.dispose();
            String filename = "photo-" + index + ".png";
            ImageIO.write(photo, "png", directory.resolve(filename).toFile());
            AssetRow asset = new AssetRow(); asset.storageKey = filename; photos.add(asset);
        }
        for (int count = 1; count <= 3; count++) {
            String key = storage.makePostcardResult("postcard-" + count, photos.subList(0, count));
            BufferedImage result = ImageIO.read(directory.resolve(key).toFile());
            assertEquals(1200, result.getWidth());
            assertEquals(1200, result.getHeight());
            assertEquals(Color.RED.getRed(), new Color(result.getRGB(300, 600)).getRed(), 5);
            if (count == 3) {
                assertTrue(new Color(result.getRGB(900, 300)).getGreen() > 100);
                assertTrue(new Color(result.getRGB(900, 900)).getBlue() > 100);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> storage.makePostcardResult("empty", List.of()));
    }

    /** 旅行明信片只接受一至三张素材。 */
    @Test void postcardTaskAcceptsOneToThreePhotos() {
        TemplateRow template = new TemplateRow();
        template.id = "travel-postcard"; template.title = "旅行明信片";
        template.requiredImageCount = 1; template.maxImageCount = 3;
        template.acceptedMimeTypes = "image/jpeg,image/png"; template.maxFileSizeBytes = 5L * 1024 * 1024;
        TemplateMapper templateMapper = proxy(TemplateMapper.class, (target, method, args) -> template);
        AssetRow asset = new AssetRow(); asset.mimeType = "image/jpeg"; asset.sizeBytes = 100; asset.storageKey = "photo.jpg";
        AssetMapper assetMapper = proxy(AssetMapper.class, (target, method, args) -> asset);
        List<Integer> insertedSlots = new ArrayList<>();
        TaskMapper taskMapper = proxy(TaskMapper.class, (target, method, args) -> {
            if (method.getName().equals("insertAsset")) insertedSlots.add((Integer) args[1]);
            return null;
        });
        TaskService service = new TaskService(templateMapper, assetMapper, taskMapper, new FakeStorage(), null, true);
        List<String> ids = List.of("first", "second", "third", "fourth");
        assertThrows(IllegalArgumentException.class, () -> service.create(new TaskService.CreateRequest(template.id, List.of(), null), 1));
        for (int count = 1; count <= 3; count++) {
            TransactionSynchronizationManager.initSynchronization();
            try {
                assertEquals("QUEUED", service.create(new TaskService.CreateRequest(template.id, ids.subList(0, count), null), 1).status());
            } finally { TransactionSynchronizationManager.clearSynchronization(); }
        }
        assertEquals(List.of(0, 0, 1, 0, 1, 2), insertedSlots);
        assertThrows(IllegalArgumentException.class, () -> service.create(new TaskService.CreateRequest(template.id, ids, null), 1));
    }

    /** 成功与失败列表使用独立查询，并返回未受分页限制的状态数量。 */
    @Test void taskHistorySeparatesSucceededAndFailed() {
        List<String> queriedStatuses = new ArrayList<>();
        Map<String, Long> totals = new HashMap<>();
        totals.put("ALL", 7L);
        totals.put("ACTIVE", 2L);
        totals.put("SUCCEEDED", 3L);
        totals.put("FAILED", 2L);
        TaskMapper taskMapper = proxy(TaskMapper.class, (target, method, args) -> switch (method.getName()) {
            case "count" -> {
                assertEquals(1L, args[0]);
                yield totals.get(args[1] == null ? "ALL" : args[1]);
            }
            case "list" -> {
                assertEquals(1L, args[0]);
                assertEquals(1, args[2]);
                assertEquals(1, args[3]);
                String status = (String) args[1];
                queriedStatuses.add(status == null ? "ALL" : status);
                TaskRow row = new TaskRow();
                row.id = "listed-task";
                row.status = status == null || "ACTIVE".equals(status) ? "QUEUED" : status;
                row.createdAt = LocalDateTime.now();
                yield List.of(row);
            }
            case "steps" -> List.of();
            default -> throw new AssertionError("Unexpected mapper call: " + method.getName());
        });
        TaskService service = new TaskService(null, null, taskMapper, null, null, true);
        for (String status : List.of("SUCCEEDED", "FAILED", "ACTIVE")) {
            TaskService.TaskPage page = service.list(1L, 1, 1, status);
            assertEquals("ACTIVE".equals(status) ? "QUEUED" : status, page.items().get(0).status());
            assertEquals(totals.get(status), page.total());
            assertEquals(new TaskService.TaskCounts(7, 2, 3, 2), page.counts());
        }
        assertEquals(7, service.list(1L, 1, 1, null).total());
        assertEquals(List.of("SUCCEEDED", "FAILED", "ACTIVE", "ALL"), queriedStatuses);
        assertThrows(IllegalArgumentException.class, () -> service.list(1L, 0, 20, "RUNNING"));
    }
}
