package com.pixforge;

import static org.junit.jupiter.api.Assertions.*;

import com.pixforge.api.ResourceUnavailableException;
import com.pixforge.data.*;
import com.pixforge.service.*;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
