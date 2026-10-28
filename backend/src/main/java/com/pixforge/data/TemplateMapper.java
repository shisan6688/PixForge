package com.pixforge.data;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateMapper {
    @Select("SELECT t.*, COALESCE(l.max_image_count, t.required_image_count) AS max_image_count FROM template t LEFT JOIN template_image_limit l ON l.template_id = t.id WHERE t.is_enabled = 1 AND (#{category} IS NULL OR t.category = #{category}) ORDER BY t.id")
    List<TemplateRow> list(@Param("category") String category);

    @Select("SELECT t.*, COALESCE(l.max_image_count, t.required_image_count) AS max_image_count FROM template t LEFT JOIN template_image_limit l ON l.template_id = t.id WHERE t.id = #{id} AND t.is_enabled = 1")
    TemplateRow find(@Param("id") String id);

    @Select("SELECT DISTINCT category FROM template WHERE is_enabled = 1 ORDER BY category")
    List<String> categories();
}
