package org.devlive.metaforge.plugin.configure;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述一个插件可配置字段。所有插件类型（Executor / Scheduler / FS / …）共用。
 * - name:         字段 key，序列化到 JSON 配置中的属性名
 * - type:         字段类型，决定 UI 渲染（文本框 / 数字 / 开关 / 密码）
 * - defaultValue: 默认值的字符串表示；调用方按 type 解析
 * - description:  字段说明，UI tooltip 用
 * - tunable:      true = 普通用户在触发任务时可临时覆盖；false = 仅管理员可在系统配置改
 *
 * Generic configurable-field descriptor used by any plugin category.
 * - tunable=false fields are administrator-only.
 * - tunable=true fields can also be overridden by end users at task invocation time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginConfigureField
{
    private String name;
    private PluginFieldType type;
    private String defaultValue;
    private String description;
    private boolean tunable;

    public PluginConfigureField(String name, PluginFieldType type, String defaultValue, String description)
    {
        this(name, type, defaultValue, description, false);
    }
}
