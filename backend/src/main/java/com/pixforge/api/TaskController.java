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

/** 处理制作任务的提交、列表、详情和结果读取。 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    private final TaskMapper mapper;
    private final StorageService storage;
    private final CurrentUser currentUser;
    /** 注入任务业务逻辑、结果存储与当前会话用户解析器。 */
    public TaskController(TaskService service, TaskMapper mapper, StorageService storage, CurrentUser currentUser) {
        this.service = service; this.mapper = mapper; this.storage = storage; this.currentUser = currentUser;
    }

    /** 受理当前用户的制作任务并返回任务快照。 */
    @PostMapping
    public ResponseEntity<TaskService.TaskView> create(@RequestBody TaskService.CreateRequest request, HttpSession session) {
        return ResponseEntity.accepted().body(service.create(request, currentUser.id(session)));
    }

    /** 按 ALL、ACTIVE、SUCCEEDED 或 FAILED 查询当前用户任务及各状态数量。 */
    @GetMapping
    public TaskService.TaskPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String status, HttpSession session) {
        return service.list(currentUser.id(session), page, size, status);
    }

    /** 返回当前用户指定任务的详情。 */
    @GetMapping("/{id}")
    public TaskService.TaskView get(@PathVariable String id, HttpSession session) {
        return service.get(id, currentUser.id(session));
    }

    /** 仅向任务所有者返回已成功制作的结果图片。 */
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
