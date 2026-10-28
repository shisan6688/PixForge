package com.pixforge.api;

import com.pixforge.data.TemplateMapper;
import com.pixforge.data.TemplateRow;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    public record Template(String id, String title, String category, String description, String coverUrl,
                           String resultSampleUrl, int requiredImageCount, int maxImageCount, List<String> acceptedMimeTypes,
                           long maxFileSizeBytes, String accent, String badge) {}
    private final TemplateMapper mapper;

    public TemplateController(TemplateMapper mapper) { this.mapper = mapper; }

    public static Template view(TemplateRow row) {
        return new Template(row.id, row.title, row.category, row.description, row.coverPath,
            row.resultSamplePath, row.requiredImageCount, row.maxImageCount, Arrays.asList(row.acceptedMimeTypes.split(",")),
            row.maxFileSizeBytes, row.accent, row.badge);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String category) {
        return Map.of("categories", mapper.categories(),
            "items", mapper.list(category == null || category.isBlank() ? null : category).stream().map(TemplateController::view).toList());
    }

    @GetMapping("/{id}")
    public Template detail(@PathVariable String id) {
        TemplateRow row = mapper.find(id);
        if (row == null) throw new ResourceUnavailableException("未找到该模板");
        return view(row);
    }
}
