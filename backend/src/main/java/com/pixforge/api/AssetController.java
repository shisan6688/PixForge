package com.pixforge.api;

import com.pixforge.data.AssetMapper;
import com.pixforge.data.AssetRow;
import com.pixforge.service.StorageService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 处理用户素材上传与私有图片读取。 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetMapper mapper;
    private final StorageService storage;
    private final CurrentUser currentUser;

    /** 注入素材持久化、文件存储与当前用户解析器。 */
    public AssetController(AssetMapper mapper, StorageService storage, CurrentUser currentUser) {
        this.mapper = mapper; this.storage = storage; this.currentUser = currentUser;
    }

    /** 验证并保存当前用户上传的图片。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, HttpSession session) throws IOException {
        AssetRow asset = new AssetRow();
        asset.id = UUID.randomUUID().toString(); asset.userId = currentUser.id(session);
        asset.originalFilename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        asset.mimeType = file.getContentType(); asset.sizeBytes = file.getSize();
        asset.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        asset.storageKey = storage.save(asset.id, file);
        mapper.insert(asset);
        return Map.of("id", asset.id, "previewUrl", "/api/assets/" + asset.id + "/content",
            "originalFilename", asset.originalFilename, "mimeType", asset.mimeType, "sizeBytes", asset.sizeBytes);
    }

    /** 仅向素材所有者返回图片内容。 */
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable String id, HttpSession session) {
        AssetRow asset = mapper.owned(id, currentUser.id(session));
        if (asset == null || !storage.exists(asset.storageKey)) throw new ResourceUnavailableException("素材不可用");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.mimeType))
            .header("Cache-Control", "private, no-store").body(storage.resource(asset.storageKey));
    }
}
