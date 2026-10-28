package com.pixforge.data;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 读写用户上传素材的数据库映射。 */
@Mapper
public interface AssetMapper {
    /** 插入素材元数据。 */
    @Insert("INSERT INTO asset (id,user_id,original_filename,mime_type,size_bytes,storage_key,created_at) VALUES (#{id},#{userId},#{originalFilename},#{mimeType},#{sizeBytes},#{storageKey},#{createdAt})")
    void insert(AssetRow row);

    /** 仅查询属于指定用户的素材。 */
    @Select("SELECT * FROM asset WHERE id = #{id} AND user_id = #{userId}")
    AssetRow owned(@Param("id") String id, @Param("userId") long userId);
}
