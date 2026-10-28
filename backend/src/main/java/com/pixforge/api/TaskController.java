package com.pixforge.api;

import com.pixforge.data.TaskMapper;
import com.pixforge.data.TaskRow;
import com.pixforge.service.StorageService;
import com.pixforge.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    private final TaskMapper mapper;
    private final StorageService storage;
    private final CurrentUser currentUser;
    public TaskController(TaskService service, TaskMapper mapper, StorageService storage, CurrentUser currentUser) {
        this.service = service; this.mapper = mapper; this.storage = storage; this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<TaskService.TaskView> create(@RequestBody TaskService.CreateRequest request, HttpSession session) {
        return ResponseEntity.accepted().body(service.create(request, currentUser.id(session)));
    }

    @GetMapping
    public TaskService.TaskPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String status, HttpSession session) {
        if (status != null && !"ACTIVE".equals(status)) throw new IllegalArgumentException("不支持的状态筛选");
        return service.list(currentUser.id(session), page, size, "ACTIVE".equals(status));
    }

    @GetMapping("/{id}")
    public TaskService.TaskView get(@PathVariable String id, HttpSession session) {
        return service.get(id, currentUser.id(session));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<Resource> result(@PathVariable String id, HttpSession session) {
        TaskRow task = mapper.owned(id, currentUser.id(session));
        if (task == null || !"SUCCEEDED".equals(task.status) || task.resultStorageKey == null || !storage.exists(task.resultStorageKey))
            throw new ResourceUnavailableException("结果不可用");
        MediaType type = task.resultStorageKey.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).header("Cache-Control", "private, no-store")
            .body(storage.resource(task.resultStorageKey));
    }
}
