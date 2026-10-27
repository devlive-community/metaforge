package org.devlive.metaforge.plugin.configure;

/**
 * 插件可配置字段的类型，决定 UI 渲染方式与默认值的解析方式。
 * 故意保持最小集，所有插件类型（Executor / Scheduler / FS / Notify …）通用。
 *
 * Lightweight field type shared by all plugin categories.
 * Kept minimal on purpose so any plugin layer can declare configurable fields without
 * pulling in business-side enums.
 */
public enum RuntimeConfigureFieldType
{
    STRING,
    NUMBER,
    BOOLEAN,
    PASSWORD
}
