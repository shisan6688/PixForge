package com.pixforge.service;

import com.pixforge.data.AssetRow;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 管理本地素材与制作结果文件。 */
@Service
public class StorageService {
    private final Path root;

    /** 设置并初始化本地文件存储根目录。 */
    public StorageService(@Value("${pixforge.storage-root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    /** 将上传图片保存到受控目录。 */
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

    /** 复制源图作为普通模板的模拟结果。 */
    public String makeResult(String taskId, AssetRow source) throws IOException {
        String extension = "image/png".equals(source.mimeType) ? ".png" : ".jpg";
        String key = "results/" + taskId + extension;
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(resolve(source.storageKey), target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    /** 将一至三张图片排版成旅行明信片拼图。 */
    public String makePostcardResult(String taskId, List<AssetRow> sources) throws IOException {
        if (sources.size() < 1 || sources.size() > 3) throw new IllegalArgumentException("旅行明信片需要 1–3 张图片");
        BufferedImage canvas = new BufferedImage(1200, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setColor(new Color(248, 242, 231));
            graphics.fillRect(0, 0, 1200, 1200);
            int margin = 36, gap = 18, area = 1200 - 2 * margin;
            if (sources.size() == 1) {
                drawPhoto(graphics, sources.get(0), margin, margin, area, area);
            } else if (sources.size() == 2) {
                int width = (area - gap) / 2;
                drawPhoto(graphics, sources.get(0), margin, margin, width, area);
                drawPhoto(graphics, sources.get(1), margin + width + gap, margin, width, area);
            } else {
                int leftWidth = (area - gap) / 2;
                int rightWidth = area - gap - leftWidth;
                int rowHeight = (area - gap) / 2;
                drawPhoto(graphics, sources.get(0), margin, margin, leftWidth, area);
                drawPhoto(graphics, sources.get(1), margin + leftWidth + gap, margin, rightWidth, rowHeight);
                drawPhoto(graphics, sources.get(2), margin + leftWidth + gap, margin + rowHeight + gap, rightWidth, rowHeight);
            }
        } finally { graphics.dispose(); }
        String key = "results/" + taskId + ".jpg";
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        if (!ImageIO.write(canvas, "jpg", target.toFile())) throw new IOException("无法写入明信片结果");
        return key;
    }

    /** 裁切并绘制一张照片到指定画框。 */
    private void drawPhoto(Graphics2D graphics, AssetRow asset, int x, int y, int width, int height) throws IOException {
        BufferedImage image = ImageIO.read(resolve(asset.storageKey).toFile());
        if (image == null) throw new IOException("素材图片无法读取");
        graphics.setColor(new Color(214, 201, 181));
        graphics.fillRect(x + 7, y + 9, width, height);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(x, y, width, height);
        int inset = 14, photoWidth = width - 2 * inset, photoHeight = height - 2 * inset;
        double scale = Math.max(photoWidth / (double) image.getWidth(), photoHeight / (double) image.getHeight());
        int cropWidth = Math.min(image.getWidth(), (int) Math.round(photoWidth / scale));
        int cropHeight = Math.min(image.getHeight(), (int) Math.round(photoHeight / scale));
        int sourceX = (image.getWidth() - cropWidth) / 2;
        int sourceY = (image.getHeight() - cropHeight) / 2;
        graphics.drawImage(image, x + inset, y + inset, x + inset + photoWidth, y + inset + photoHeight,
            sourceX, sourceY, sourceX + cropWidth, sourceY + cropHeight, null);
    }

    /** 检查存储键对应的文件是否存在。 */
    public boolean exists(String key) { return Files.isRegularFile(resolve(key)); }
    /** 将存储键转换为 Spring 可读取资源。 */
    public Resource resource(String key) { return new FileSystemResource(resolve(key)); }

    /** 解析并限制文件路径在存储根目录内。 */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("无效的存储路径");
        return path;
    }
}
