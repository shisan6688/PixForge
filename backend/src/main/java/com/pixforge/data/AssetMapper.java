package com.pixforge.data;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssetMapper {
    @Insert("INSERT INTO asset (id,user_id,original_filename,mime_type,size_bytes,storage_key,created_at) VALUES (#{id},#{userId},#{originalFilename},#{mimeType},#{sizeBytes},#{storageKey},#{createdAt})")
    void insert(AssetRow row);

    @Select("SELECT * FROM asset WHERE id = #{id} AND user_id = #{userId}")
    AssetRow owned(@Param("id") String id, @Param("userId") long userId);
}
