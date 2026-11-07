package org.devlive.metaforge.plugin.configure;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 描述一个插件可配置字段。所有插件类型（Executor / Scheduler / FS / …）共用。
 * - name:         字段 key，序列化到 JSON 配置中的属性名
 * - type:         字段类型，决定 UI 渲染（文本框 / 数字 / 开关 / 密码 / 下拉）
 * - defaultValue: 默认值的字符串表示；调用方按 type 解析
 * - description:  字段说明，UI tooltip 用
 * - tunable:      true = 普通用户在触发任务时可临时覆盖；false = 仅管理员可在系统配置改
 * - options:      SELECT 类型的可选值
 * - group:        字段分组 key，UI 按组展示；为空表示不分组
 *
 * Generic configurable-field descriptor used by any plugin category.
 * - tunable=false fields are administrator-only.
 * - tunable=true fields can also be overridden by end users at task invocation time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RuntimeConfigureField
{
    private String name;
    private RuntimeConfigureFieldType type;
    private String defaultValue;
    private String description;
    private boolean tunable;
    private List<String> options;
    private String group;

    public RuntimeConfigureField(String name, RuntimeConfigureFieldType type, String defaultValue, String description)
    {
        this(name, type, defaultValue, description, false);
    }

    public RuntimeConfigureField(String name, RuntimeConfigureFieldType type, String defaultValue, String description, boolean tunable)
    {
        this(name, type, defaultValue, description, tunable, null, null);
    }

    public RuntimeConfigureField(String name, RuntimeConfigureFieldType type, String defaultValue, String description, boolean tunable, String group)
    {
        this(name, type, defaultValue, description, tunable, null, group);
    }
}
