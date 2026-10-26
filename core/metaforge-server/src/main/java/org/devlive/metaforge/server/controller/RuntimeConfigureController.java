package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.service.configure.DatasetTargetSchema;
import org.devlive.metaforge.service.entity.ConfigureEntity;
import org.devlive.metaforge.service.initializer.DataSetConfigure;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only CRUD for runtime configuration rows (metaforge_configure).
 * - GET  /list/{category}            list all rows of a category
 * - GET  /detail/{category}/{name}   fetch effective values + schema for one row
 * - PUT  /save/{category}/{name}     save the row (admin only by route guard / role)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/configure/runtime")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class RuntimeConfigureController
{
    private final PluginManager pluginManager;
    private final RuntimeConfigureService service;
    private final DataSetConfigure dataSetConfigure;

    public RuntimeConfigureController(PluginManager pluginManager, RuntimeConfigureService service, DataSetConfigure dataSetConfigure)
    {
        this.pluginManager = pluginManager;
        this.service = service;
        this.dataSetConfigure = dataSetConfigure;
    }

    @GetMapping(value = "list/{category}")
    public CommonResponse<List<ConfigureEntity>> list(@PathVariable String category)
    {
        return CommonResponse.success(service.list(category));
    }

    @GetMapping(value = "detail/{category}/{name}")
    public CommonResponse<DetailView> detail(@PathVariable String category, @PathVariable String name)
    {
        List<PluginConfigureField> schema = resolveSchema(category, name);
        if (schema == null) {
            return CommonResponse.failure("No schema for " + category + "/" + name);
        }
        Map<String, String> effective = service.getEffective(category, name, schema);
        return CommonResponse.success(new DetailView(category, name, schema, effective));
    }

    @PutMapping(value = "save/{category}/{name}")
    public CommonResponse<ConfigureEntity> save(
            @PathVariable String category,
            @PathVariable String name,
            @RequestBody Map<String, String> configMap)
    {
        // 校验：只保留 schema 中存在的 key（防止脏字段写入）
        List<PluginConfigureField> schema = resolveSchema(category, name);
        Map<String, String> sanitized = new LinkedHashMap<>();
        if (schema != null) {
            for (PluginConfigureField field : schema) {
                if (configMap != null && configMap.containsKey(field.getName())) {
                    sanitized.put(field.getName(), configMap.get(field.getName()));
                }
            }
        }
        CommonResponse<ConfigureEntity> response = service.save(category, name, sanitized, null);
        // 写完热刷新 DataSetConfigure（dataset target 类配置）
        if (Boolean.TRUE.equals(response.getStatus())
                && RuntimeConfigureService.CATEGORY_DATASET.equals(category)
                && DatasetTargetSchema.NAME.equals(name)) {
            try {
                dataSetConfigure.reload();
            }
            catch (Exception ex) {
                log.warn("Reload dataset target configure failed: {}", ex.getMessage());
            }
        }
        return response;
    }

    private List<PluginConfigureField> resolveSchema(String category, String name)
    {
        if (RuntimeConfigureService.CATEGORY_EXECUTOR.equals(category)) {
            return pluginManager.getPluginInfos().stream()
                    .filter(m -> m.getType() == PluginType.EXECUTOR)
                    .map(m -> m.getInstance())
                    .filter(i -> i instanceof Plugin)
                    .map(i -> (Plugin) i)
                    .filter(p -> name.equals(p.getName()))
                    .findFirst()
                    .map(Plugin::configures)
                    .orElse(null);
        }
        if (RuntimeConfigureService.CATEGORY_DATASET.equals(category) && DatasetTargetSchema.NAME.equals(name)) {
            return DatasetTargetSchema.fields();
        }
        return Collections.emptyList();
    }

    @SuppressFBWarnings(value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class DetailView
    {
        public String category;
        public String name;
        public List<PluginConfigureField> schema;
        public Map<String, String> values;

        public DetailView(String category, String name, List<PluginConfigureField> schema, Map<String, String> values)
        {
            this.category = category;
            this.name = name;
            this.schema = schema == null ? new ArrayList<>() : schema;
            this.values = values;
        }
    }
}
