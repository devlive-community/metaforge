package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.adhoc.Adhoc;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.spi.model.Response;

import java.util.List;
import java.util.Set;

public interface DataSetService
        extends BaseService<DataSetEntity>
{
    CommonResponse<Long> count();

    CommonResponse<Response> preview(String code);

    CommonResponse<DataSetEntity> rebuild(String code);

    CommonResponse<List<DataSetColumnEntity>> getColumnsByCode(String code);

    /**
     * 提交一次同步，返回新建的同步历史（进入排队）。数据集已有排队或运行中的同步时失败。
     * Submit a sync and return the new, queued history row; fails while the dataset already has one queued or running.
     */
    CommonResponse<DatasetHistoryEntity> syncData(String code);

    /**
     * 同步并允许调用方临时覆盖一组 tunable 字段；非 tunable 的字段会被忽略。
     * Trigger sync with optional per-invocation overrides; only tunable fields are honored.
     */
    CommonResponse<DatasetHistoryEntity> syncData(String code, java.util.Map<String, String> overrides);

    /**
     * 返回当前 executor 的可调字段（tunable=true），defaultValue 已替换为该数据集对应 executor 的 effective 值，
     * 供同步对话框预填表单使用。
     * Return the executor's tunable fields with defaultValue pre-populated from current effective config.
     */
    CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField>> getSyncFields(String code);

    CommonResponse<Boolean> clearData(String code);

    CommonResponse<Response> adhoc(String code, Adhoc configure);

    CommonResponse<Set<PluginMetadata>> getActuators();

    CommonResponse<DataSetEntity> getInfo(String code);

    CommonResponse<PageEntity<DatasetHistoryEntity>> getHistory(String code, FilterBody filter);

    /**
     * 读取指定同步历史的运行日志（独立任务日志文件，由 executor 写入 workHome）
     * Read the executor's task log for the given sync history record
     */
    CommonResponse<List<String>> getHistoryLog(Long id);

    /**
     * 取消同步：排队中的直接取消；运行中的请求执行器停止（仅对当前进程持有的任务生效）
     * Cancel a sync: a queued one is cancelled at once, a running one is asked to stop (only on the node running it)
     */
    CommonResponse<Boolean> stopHistory(Long id);

    /**
     * 一次同步的实时指标、状态与状态转换；已结束的返回落库的最终结果
     * Live metrics, state and transitions of one sync; the persisted final result once it has finished
     */
    CommonResponse<org.devlive.metaforge.service.dataset.sync.SyncMetricsView> getHistoryMetrics(Long id);

    /**
     * 暂停运行中的同步，提交完已成形的批次后保存断点。只有按键分片的同步能暂停。
     * Pause a running sync; it saves a checkpoint after committing the batches already formed.
     */
    CommonResponse<Boolean> pauseHistory(Long id);

    /**
     * 当前用户未结束的同步，供列表页显示进度。
     * The signed-in user's unfinished syncs, for the progress on the list page.
     */
    CommonResponse<List<org.devlive.metaforge.service.dataset.sync.SyncActiveView>> getActiveSyncs();

    /**
     * 继续已暂停的同步，回到队列后从断点续传。
     * Resume a paused sync: back into the queue, continuing from its checkpoint.
     */
    CommonResponse<Boolean> resumeHistory(Long id);

    /**
     * 让失败、超时、停止或中断的同步再次运行，只在不会重复写入时允许。
     * Run a failed, timed-out, stopped or interrupted sync again, only when that writes nothing twice.
     */
    CommonResponse<Boolean> retryHistory(Long id);
}
