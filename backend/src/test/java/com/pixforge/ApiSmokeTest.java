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
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ApiSmokeTest {
    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private TaskRow task(String failStage) {
        TaskRow row = new TaskRow();
        row.id = "task-1"; row.status = "QUEUED"; row.simulationDelayMs = 0;
        row.simulationFailStage = failStage;
        return row;
    }

    private TaskMapper mapper(TaskRow task, AssetRow asset) {
        return proxy(TaskMapper.class, (target, method, args) -> switch (method.getName()) {
            case "find" -> task;
            case "assets" -> List.of(asset);
            case "owned" -> null;
            default -> throw new AssertionError("Unexpected mapper call: " + method.getName());
        });
    }

    static class RecordingState extends TaskStateService {
        final List<String> events = new ArrayList<>();
        RecordingState(TaskMapper mapper) { super(mapper); }
        @Override public boolean start(String id, String stage) { events.add("start:" + stage); return true; }
        @Override public void finish(String id, String stage, String result) { events.add("finish:" + stage + ":" + result); }
        @Override public void fail(String id, String stage, String code, String message) { events.add("fail:" + stage + ":" + code); }
    }

    static class FakeStorage extends StorageService {
        boolean copied;
        FakeStorage() { super("data"); }
        @Override public boolean exists(String key) { return true; }
        @Override public String makeResult(String id, AssetRow asset) throws IOException { copied = true; return "results/task-1.jpg"; }
    }

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

    @Test void simulatedFailureStopsLaterStages() {
        AssetRow asset = new AssetRow(); asset.storageKey = "assets/a.jpg";
        TaskMapper mapper = mapper(task("GENERATE"), asset);
        RecordingState state = new RecordingState(mapper);
        FakeStorage storage = new FakeStorage();
        TaskProcessor processor = new TaskProcessor(mapper, state, storage, 1, 1);
        try {
            processor.process("task-1");
            assertEquals(List.of("start:CHECK_ASSETS", "finish:CHECK_ASSETS:null",
                "start:GENERATE", "fail:GENERATE:SIMULATED_FAILURE"), state.events);
            assertFalse(storage.copied);
        } finally { processor.shutdown(); }
    }

    @Test void terminalTaskIsNeverRestarted() {
        TaskMapper mapper = proxy(TaskMapper.class, (target, method, args) -> {
            if (method.getName().equals("start")) return 0;
            throw new AssertionError("Terminal task should not transition");
        });
        assertFalse(new TaskStateService(mapper).start("task-1", "CHECK_ASSETS"));
    }

    @Test void userCannotReadAnotherUsersTask() {
        TaskMapper mapper = mapper(task(null), new AssetRow());
        TaskService service = new TaskService(null, null, mapper, null, null, true);
        assertThrows(ResourceUnavailableException.class, () -> service.get("private-task", 2L));
    }

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
}
