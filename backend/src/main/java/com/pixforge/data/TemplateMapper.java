package com.pixforge.data;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 查询启用模板及素材数量限制的数据库映射。 */
@Mapper
public interface TemplateMapper {
    /** 按可选分类查询启用模板。 */
    @Select("SELECT t.*, COALESCE(l.max_image_count, t.required_image_count) AS max_image_count FROM template t LEFT JOIN template_image_limit l ON l.template_id = t.id WHERE t.is_enabled = 1 AND (#{category} IS NULL OR t.category = #{category}) ORDER BY t.id")
    List<TemplateRow> list(@Param("category") String category);

    /** 按 ID 查询启用模板。 */
    @Select("SELECT t.*, COALESCE(l.max_image_count, t.required_image_count) AS max_image_count FROM template t LEFT JOIN template_image_limit l ON l.template_id = t.id WHERE t.id = #{id} AND t.is_enabled = 1")
    TemplateRow find(@Param("id") String id);

    /** 查询启用模板的全部分类。 */
    @Select("SELECT DISTINCT category FROM template WHERE is_enabled = 1 ORDER BY category")
    List<String> categories();
}
