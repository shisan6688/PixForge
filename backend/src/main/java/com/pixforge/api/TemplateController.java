package com.pixforge.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    public record Template(String id, String title, String category, String description, String coverUrl,
                           String resultSampleUrl, int requiredImageCount, List<String> acceptedMimeTypes,
                           long maxFileSizeBytes, String accent, String badge) {}
    private static final long FIVE_MB = 5L * 1024 * 1024;
    private static final List<Template> TEMPLATES = List.of(
        template("cute-effect", "可爱特效", "图片特效", "让日常照片多一点俏皮。柔和贴纸、闪光和温暖色调，一键点亮可爱瞬间。", "cute-effect-before-after.png", 1, "#f8b7ca", "热门"),
        template("ai-chibi", "真人变 Q 版", "AI 写真", "保留人物神态与穿搭特征，模拟生成一张有趣又精致的 Q 版形象。", "ai-chibi-before-after.png", 1, "#d6c6f7", "NEW"),
        template("travel-postcard", "旅行明信片", "创意设计", "把旅途中的风景与回忆做成一张独一无二的旅行明信片。", "letizia-agosta-3TzHmifXX5k-unsplash.jpg", 2, "#b7d9cf", null),
        template("film-memory", "胶片记忆", "图片特效", "为照片增添细腻颗粒与怀旧色彩，收藏此刻的光与影。", "sam-operchuck-LGeE_cTLoqY-unsplash.jpg", 1, "#d8c2ac", null),
        template("golden-hour", "日落剪影", "AI 写真", "用一张人像照片，模拟黄昏光线下的电影感剪影。", "cemrecan-yurtman-wih0v2QIySo-unsplash.jpg", 1, "#b6d7d1", null),
        template("vintage-camera", "复古时光", "创意设计", "组合两张照片，模拟制作一幅带有复古相机质感的回忆拼贴。", "mustafi-numann-kz3pzTVrHtU-unsplash.jpg", 2, "#e1bc99", null),
        template("misty-portrait", "晨雾人像", "AI 写真", "让人物融入轻盈的薄雾与柔和光影，模拟制作静谧氛围照片。", "cemrecan-yurtman-vqTAlVvrp2M-unsplash.jpg", 1, "#b4cfd1", null),
        template("urban-portrait", "都市街拍", "AI 写真", "用一张人像照片，模拟制作暖调城市街头写真，呈现自然的光影与街景氛围。", "pexels-bollieball-5189976.jpg", 1, "#cbb69f", "NEW")
    );

    private static Template template(String id, String title, String category, String description, String image, int count, String accent, String badge) {
        String url = "/templates/" + image;
        return new Template(id, title, category, description, url, url, count, List.of("image/jpeg", "image/png"), FIVE_MB, accent, badge);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String category) {
        return Map.of("categories", TEMPLATES.stream().map(Template::category).distinct().toList(),
                      "items", TEMPLATES.stream().filter(item -> category == null || category.isBlank() || item.category().equals(category)).toList());
    }

    @GetMapping("/{id}")
    public Template detail(@PathVariable String id) {
        return TEMPLATES.stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("未找到该模板"));
    }
}
