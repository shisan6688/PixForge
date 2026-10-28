package com.pixforge.data;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TaskMapper {
    @Insert("INSERT INTO production_task (id,user_id,template_id,status,simulation_fail_stage,simulation_delay_ms,created_at,updated_at) VALUES (#{id},#{userId},#{templateId},#{status},#{simulationFailStage},#{simulationDelayMs},#{createdAt},#{updatedAt})")
    void insert(TaskRow row);

    @Insert("INSERT INTO task_asset (task_id,slot_index,asset_id) VALUES (#{taskId},#{slot},#{assetId})")
    void insertAsset(@Param("taskId") String taskId, @Param("slot") int slot, @Param("assetId") String assetId);

    @Insert("INSERT INTO task_step (task_id,stage,step_order,status) VALUES (#{taskId},#{stage},#{order},'PENDING')")
    void insertStep(@Param("taskId") String taskId, @Param("stage") String stage, @Param("order") int order);

    @Select("SELECT t.*, p.title AS template_title, p.cover_path AS template_cover_url FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.id=#{id} AND t.user_id=#{userId}")
    TaskRow owned(@Param("id") String id, @Param("userId") long userId);

    @Select("SELECT t.*, p.title AS template_title, p.cover_path AS template_cover_url FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.id=#{id}")
    TaskRow find(@Param("id") String id);

    @Select("SELECT t.*, p.title AS template_title, p.cover_path AS template_cover_url FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.user_id=#{userId} AND (#{active} = 0 OR t.status IN ('QUEUED','RUNNING')) ORDER BY t.created_at DESC, t.id DESC LIMIT #{size} OFFSET #{offset}")
    List<TaskRow> list(@Param("userId") long userId, @Param("active") int active, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM production_task WHERE user_id=#{userId} AND (#{active} = 0 OR status IN ('QUEUED','RUNNING'))")
    long count(@Param("userId") long userId, @Param("active") int active);

    @Select("SELECT * FROM task_step WHERE task_id=#{taskId} ORDER BY step_order")
    List<StepRow> steps(@Param("taskId") String taskId);

    @Select("SELECT a.* FROM task_asset ta JOIN asset a ON a.id=ta.asset_id WHERE ta.task_id=#{taskId} ORDER BY ta.slot_index")
    List<AssetRow> assets(@Param("taskId") String taskId);

    @Update("UPDATE production_task SET status='RUNNING',current_stage=#{stage},started_at=COALESCE(started_at,#{now}),updated_at=#{now} WHERE id=#{id} AND status IN ('QUEUED','RUNNING')")
    int start(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now);

    @Update("UPDATE task_step SET status='RUNNING',started_at=#{now} WHERE task_id=#{id} AND stage=#{stage} AND status='PENDING'")
    int startStep(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now);

    @Update("UPDATE task_step SET status='SUCCEEDED',finished_at=#{now} WHERE task_id=#{id} AND stage=#{stage} AND status='RUNNING'")
    int finishStep(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now);

    @Update("UPDATE task_step SET status='FAILED',finished_at=#{now},failure_code=#{code},failure_message=#{message} WHERE task_id=#{id} AND stage=#{stage} AND status='RUNNING'")
    int failStep(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now, @Param("code") String code, @Param("message") String message);

    @Update("UPDATE production_task SET status='FAILED',failure_code=#{code},failure_message=#{message},finished_at=#{now},updated_at=#{now} WHERE id=#{id} AND status IN ('QUEUED','RUNNING')")
    int fail(@Param("id") String id, @Param("now") LocalDateTime now, @Param("code") String code, @Param("message") String message);

    @Update("UPDATE production_task SET status='SUCCEEDED',result_storage_key=#{key},finished_at=#{now},updated_at=#{now} WHERE id=#{id} AND status='RUNNING'")
    int succeed(@Param("id") String id, @Param("key") String key, @Param("now") LocalDateTime now);

    @Update("UPDATE task_step SET status='FAILED',finished_at=#{now},failure_code='SERVICE_INTERRUPTED',failure_message='服务重启，制作任务已中断' WHERE status='RUNNING'")
    int interruptSteps(@Param("now") LocalDateTime now);

    @Update("UPDATE production_task SET status='FAILED',failure_code='SERVICE_INTERRUPTED',failure_message='服务重启，制作任务已中断',finished_at=#{now},updated_at=#{now} WHERE status IN ('QUEUED','RUNNING')")
    int interruptTasks(@Param("now") LocalDateTime now);
}
