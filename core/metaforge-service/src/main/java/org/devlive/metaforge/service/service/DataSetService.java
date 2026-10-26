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
    CommonResponse<DataSetEntity> rebuild(String code);

    CommonResponse<List<DataSetColumnEntity>> getColumnsByCode(String code);

    CommonResponse<DataSetEntity> syncData(String code);

    /**
     * 同步并允许调用方临时覆盖一组 tunable 字段；非 tunable 的字段会被忽略。
     * Trigger sync with optional per-invocation overrides; only tunable fields are honored.
     */
    CommonResponse<DataSetEntity> syncData(String code, java.util.Map<String, String> overrides);

    /**
     * 返回当前 executor 的可调字段（tunable=true），defaultValue 已替换为该数据集对应 executor 的 effective 值，
     * 供同步对话框预填表单使用。
     * Return the executor's tunable fields with defaultValue pre-populated from current effective config.
     */
    CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField>> getSyncFields(String code);

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
     * 停止正在运行的同步任务。仅对 state=RUNNING 且当前进程持有该 taskName 的任务生效
     * Stop a running sync task. Effective only when state=RUNNING and the task is held by this process
     */
    CommonResponse<Boolean> stopHistory(Long id);
}
