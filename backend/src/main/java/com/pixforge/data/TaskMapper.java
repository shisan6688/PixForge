package com.pixforge.data;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** 读写制作任务、素材关联与阶段状态的数据库映射。 */
@Mapper
public interface TaskMapper {
    /** 插入新制作任务。 */
    @Insert("INSERT INTO production_task (id,user_id,template_id,status,simulation_fail_stage,simulation_delay_ms,created_at,updated_at) VALUES (#{id},#{userId},#{templateId},#{status},#{simulationFailStage},#{simulationDelayMs},#{createdAt},#{updatedAt})")
    void insert(TaskRow row);

    /** 按上传位关联任务与素材。 */
    @Insert("INSERT INTO task_asset (task_id,slot_index,asset_id) VALUES (#{taskId},#{slot},#{assetId})")
    void insertAsset(@Param("taskId") String taskId, @Param("slot") int slot, @Param("assetId") String assetId);

    /** 创建待执行的任务阶段。 */
    @Insert("INSERT INTO task_step (task_id,stage,step_order,status) VALUES (#{taskId},#{stage},#{order},'PENDING')")
    void insertStep(@Param("taskId") String taskId, @Param("stage") String stage, @Param("order") int order);

    /** 仅查询属于指定用户的任务。 */
    @Select("SELECT t.*, p.title AS template_title, p.cover_path AS template_cover_url FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.id=#{id} AND t.user_id=#{userId}")
    TaskRow owned(@Param("id") String id, @Param("userId") long userId);

    /** 按 ID 查询任务，供后台执行器使用。 */
    @Select("SELECT t.*, p.title AS template_title, p.cover_path AS template_cover_url FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.id=#{id}")
    TaskRow find(@Param("id") String id);

    /** 按用户与状态筛选分页查询任务；ACTIVE 包含排队和运行中。 */
    @Select("SELECT t.*, p.title AS template_title, p.cover_path AS template_cover_url FROM production_task t JOIN template p ON p.id=t.template_id WHERE t.user_id=#{userId} AND (#{status} IS NULL OR (#{status}='ACTIVE' AND t.status IN ('QUEUED','RUNNING')) OR t.status=#{status}) ORDER BY t.created_at DESC, t.id DESC LIMIT #{size} OFFSET #{offset}")
    List<TaskRow> list(@Param("userId") long userId, @Param("status") String status, @Param("size") int size, @Param("offset") int offset);

    /** 统计用户指定状态的任务数量；空状态统计全部任务。 */
    @Select("SELECT COUNT(*) FROM production_task WHERE user_id=#{userId} AND (#{status} IS NULL OR (#{status}='ACTIVE' AND status IN ('QUEUED','RUNNING')) OR status=#{status})")
    long count(@Param("userId") long userId, @Param("status") String status);

    /** 按执行顺序查询任务阶段。 */
    @Select("SELECT * FROM task_step WHERE task_id=#{taskId} ORDER BY step_order")
    List<StepRow> steps(@Param("taskId") String taskId);

    /** 按上传位顺序查询任务素材。 */
    @Select("SELECT a.* FROM task_asset ta JOIN asset a ON a.id=ta.asset_id WHERE ta.task_id=#{taskId} ORDER BY ta.slot_index")
    List<AssetRow> assets(@Param("taskId") String taskId);

    /** 将排队或运行中的任务设置为当前执行阶段。 */
    @Update("UPDATE production_task SET status='RUNNING',current_stage=#{stage},started_at=COALESCE(started_at,#{now}),updated_at=#{now} WHERE id=#{id} AND status IN ('QUEUED','RUNNING')")
    int start(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now);

    /** 启动指定的待执行阶段。 */
    @Update("UPDATE task_step SET status='RUNNING',started_at=#{now} WHERE task_id=#{id} AND stage=#{stage} AND status='PENDING'")
    int startStep(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now);

    /** 将运行中的阶段标记为成功。 */
    @Update("UPDATE task_step SET status='SUCCEEDED',finished_at=#{now} WHERE task_id=#{id} AND stage=#{stage} AND status='RUNNING'")
    int finishStep(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now);

    /** 记录运行阶段的失败原因。 */
    @Update("UPDATE task_step SET status='FAILED',finished_at=#{now},failure_code=#{code},failure_message=#{message} WHERE task_id=#{id} AND stage=#{stage} AND status='RUNNING'")
    int failStep(@Param("id") String id, @Param("stage") String stage, @Param("now") LocalDateTime now, @Param("code") String code, @Param("message") String message);

    /** 将未结束的任务标记为失败。 */
    @Update("UPDATE production_task SET status='FAILED',failure_code=#{code},failure_message=#{message},finished_at=#{now},updated_at=#{now} WHERE id=#{id} AND status IN ('QUEUED','RUNNING')")
    int fail(@Param("id") String id, @Param("now") LocalDateTime now, @Param("code") String code, @Param("message") String message);

    /** 保存结果路径并将运行任务标记为成功。 */
    @Update("UPDATE production_task SET status='SUCCEEDED',result_storage_key=#{key},finished_at=#{now},updated_at=#{now} WHERE id=#{id} AND status='RUNNING'")
    int succeed(@Param("id") String id, @Param("key") String key, @Param("now") LocalDateTime now);

    /** 服务启动时将遗留的运行阶段标记为中断。 */
    @Update("UPDATE task_step SET status='FAILED',finished_at=#{now},failure_code='SERVICE_INTERRUPTED',failure_message='服务重启，制作任务已中断' WHERE status='RUNNING'")
    int interruptSteps(@Param("now") LocalDateTime now);

    /** 服务启动时将遗留的未结束任务标记为中断。 */
    @Update("UPDATE production_task SET status='FAILED',failure_code='SERVICE_INTERRUPTED',failure_message='服务重启，制作任务已中断',finished_at=#{now},updated_at=#{now} WHERE status IN ('QUEUED','RUNNING')")
    int interruptTasks(@Param("now") LocalDateTime now);
}
