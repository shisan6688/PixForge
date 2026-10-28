package com.pixforge.service;

import com.pixforge.data.AssetRow;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private final Path root;

    public StorageService(@Value("${pixforge.storage-root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public String save(String id, MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("图片大小须在 1 字节至 5 MB 之间");
        byte[] bytes = file.getBytes();
        String type = file.getContentType();
        boolean jpeg = bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
        boolean png = bytes.length > 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71;
        if (!(jpeg && "image/jpeg".equals(type) || png && "image/png".equals(type)) || ImageIO.read(new ByteArrayInputStream(bytes)) == null)
            throw new IllegalArgumentException("只接受有效的 JPEG 或 PNG 图片");
        String key = "assets/" + id + (jpeg ? ".jpg" : ".png");
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return key;
    }

    public String makeResult(String taskId, AssetRow source) throws IOException {
        String extension = "image/png".equals(source.mimeType) ? ".png" : ".jpg";
        String key = "results/" + taskId + extension;
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(resolve(source.storageKey), target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    public boolean exists(String key) { return Files.isRegularFile(resolve(key)); }
    public Resource resource(String key) { return new FileSystemResource(resolve(key)); }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("无效的存储路径");
        return path;
    }
}
