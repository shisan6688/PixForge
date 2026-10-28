package com.pixforge.data;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateMapper {
    @Select("SELECT * FROM template WHERE is_enabled = 1 AND (#{category} IS NULL OR category = #{category}) ORDER BY id")
    List<TemplateRow> list(@Param("category") String category);

    @Select("SELECT * FROM template WHERE id = #{id} AND is_enabled = 1")
    TemplateRow find(@Param("id") String id);

    @Select("SELECT DISTINCT category FROM template WHERE is_enabled = 1 ORDER BY category")
    List<String> categories();
}
