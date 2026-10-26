package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.service.entity.ConfigureEntity;

import java.util.List;
import java.util.Map;

/**
 * 运行时可写配置（metaforge_configure 表）读写服务。
 * 与 ConfigureService（读 executor YAML 元数据，UI 分类用）语义不同：
 * 这里管的是 Executor / Dataset Target 真正生效的运行参数。
 *
 * Read / write runtime configuration backed by metaforge_configure.
 * Field schema comes from Plugin.configures(); the service merges DB row over schema defaults
 * so callers always get a complete value map.
 */
public interface RuntimeConfigureService
{
    String CATEGORY_EXECUTOR = "EXECUTOR";
    String CATEGORY_DATASET = "DATASET";

    /**
     * 合并 DB 行与 schema 默认值，返回该 (category, name) 的完整 effective 配置。
     * - DB 行存在的字段使用 DB 值；缺失字段回落 schema.defaultValue
     * - 字符串形式返回；调用方根据 schema 类型按需 parse
     */
    Map<String, String> getEffective(String category, String name, List<PluginConfigureField> fields);

    /** 不参考 schema 默认值，仅返回 DB 行（可能空 map），用于 UI 展示"用户改过哪些项" */
    Map<String, String> getRaw(String category, String name);

    /** 列出某范畴下的全部配置项，系统配置页用 */
    List<ConfigureEntity> list(String category);

    /** 管理员写：不存在则新建，存在则更新 configure JSON */
    CommonResponse<ConfigureEntity> save(String category, String name, Map<String, String> configMap, String description);

    /**
     * 首次启动 seed：当 (category, name) 不存在时，按 schema 默认值生成一条；存在则不动。
     * 用于把 plugin 默认值"落地"到 DB，让管理员之后可以从 UI 改。
     */
    void seedIfAbsent(String category, String name, List<PluginConfigureField> fields, String description);
}
