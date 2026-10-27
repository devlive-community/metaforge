package org.devlive.metaforge.service.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.utils.BeanToPropertiesUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.configure.FieldType;
import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutor;
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutorField;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;
import org.devlive.metaforge.plugin.configure.PluginConfigurePipelineType;
import org.devlive.metaforge.service.body.PipelineFieldBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings(value = {"SF_SWITCH_NO_DEFAULT", "SF_SWITCH_FALLTHROUGH"})
public class ConfigureUtils
{
    private ConfigureUtils()
    {}

    public static String preparedMessage(List<PluginConfigureField> configures)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(ServiceState.PLUGIN_CONFIGURE_REQUIRED.getValue());
        configures.forEach(v -> {
            buffer.append("<br/>");
            buffer.append("Field: " + v.getField());
        });
        return buffer.toString();
    }

    public static RuntimeConfigure preparedConfigure(List<PluginConfigureField> configures)
    {
        RuntimeConfigure configure = new RuntimeConfigure();
        configures.forEach(v -> {
            switch (v.getField()) {
                case host:
                    configure.setHost(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.host));
                    break;
                case port:
                    configure.setPort(ConfigureUtils.getIntegerValue(configures, PluginConfigureFieldName.port));
                    break;
                case username:
                    configure.setUsername(Optional.ofNullable(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.username)));
                    break;
                case password:
                    configure.setPassword(Optional.ofNullable(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.password)));
                    break;
                case database:
                    String database = ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.database);
                    Optional<String> _database = StringUtils.isNotEmpty(database) ? Optional.ofNullable(database) : Optional.empty();
                    configure.setDatabase(_database);
                    break;
                case ssl:
                    configure.setSsl(Optional.ofNullable(ConfigureUtils.getBooleanValue(configures, PluginConfigureFieldName.ssl)));
                    break;
                case configures:
                    configure.setEnv(Optional.ofNullable(ConfigureUtils.getMapValue(configures, PluginConfigureFieldName.configures)));
                    break;
                case file:
                    configure.setUsedConfig(true);
                    break;
            }
        });
        configure.setFormat("JsonConvert");
        return configure;
    }

    public static PluginConfigure preparedConfigure(PluginConfigure configure, SourceEntity source)
    {
        configure.getConfigures().forEach(v -> {
            switch (v.getField()) {
                case name:
                    v.setValue(source.getName());
                    break;
                case host:
                    v.setValue(source.getHost());
                    break;
                case port:
                    v.setValue(source.getPort());
                    break;
                case username:
                    v.setValue(source.getUsername());
                    break;
                case password:
                    v.setValue(source.getPassword());
                    break;
                case database:
                    v.setValue(source.getDatabase());
                    break;
                case ssl:
                    v.setValue(source.getSsl());
                    break;
                case catalog:
                    v.setValue(source.getCatalog());
                    break;
                case configures:
                    List<Map<String, Object>> fields = new ArrayList<>();
                    if (ObjectUtils.isNotEmpty(source.getConfigures())) {
                        source.getConfigures().forEach((key, value) -> {
                            if (ObjectUtils.isEmpty(key)) {
                                return;
                            }
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put(PluginConfigureFieldName.field.name(), key);
                            map.put(PluginConfigureFieldName.value.name(), value);
                            fields.add(map);
                        });
                    }
                    v.setValue(fields);
                    break;
            }
        });
        return configure;
    }

    public static SourceEntity preparedSourceEntity(List<PluginConfigureField> configures)
    {
        SourceEntity configure = new SourceEntity();
        configures.forEach(v -> {
            switch (v.getField()) {
                case name:
                    configure.setName(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.name));
                case host:
                    configure.setHost(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.host));
                    break;
                case port:
                    configure.setPort(ConfigureUtils.getIntegerValue(configures, PluginConfigureFieldName.port));
                    break;
                case username:
                    configure.setUsername(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.username));
                    break;
                case password:
                    configure.setPassword(ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.password));
                    break;
                case database:
                    String database = ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.database);
                    configure.setDatabase(database);
                    break;
                case catalog:
                    String catalog = ConfigureUtils.getStringValue(configures, PluginConfigureFieldName.catalog);
                    configure.setCatalog(catalog);
                    break;
                case ssl:
                    configure.setSsl(ConfigureUtils.getBooleanValue(configures, PluginConfigureFieldName.ssl));
                    break;
                case configures:
                    configure.setOriginalConfigure(JsonUtils.toJSON(ConfigureUtils.getMapValue(configures, PluginConfigureFieldName.configures)));
                    break;
                case file:
                    configure.setUsedConfig(true);
                    break;
            }
        });
        configure.setCreateTime(Timestamp.from(Instant.now()));
        return configure;
    }

    public static List<PluginConfigureField> filterNotEmpty(List<PluginConfigureField> configures)
    {
        return configures.stream().filter(v -> !isEmpty(v)).collect(Collectors.toList());
    }

    public static boolean isEmpty(PluginConfigureField field)
    {
        boolean flag = true;
        switch (field.getType()) {
            case String:
                if (ObjectUtils.isNotEmpty(field.getValue())) {
                    flag = StringUtils.isEmpty(String.valueOf(field.getValue()));
                }
                break;
            default:
                flag = false;
        }
        return flag;
    }

    public static PluginConfigureField getConfigure(List<PluginConfigureField> configures, PluginConfigureFieldName key)
    {
        Optional<PluginConfigureField> configureFieldOptional = configures.stream().filter(v -> v.getField().equals(key)).findFirst();
        return configureFieldOptional.get();
    }

    public static String getStringValue(List<PluginConfigureField> configures, PluginConfigureFieldName key)
    {
        return String.valueOf(getConfigure(configures, key).getValue());
    }

    public static Integer getIntegerValue(List<PluginConfigureField> configures, PluginConfigureFieldName key)
    {
        return Integer.valueOf(getStringValue(configures, key));
    }

    public static Long getLongValue(List<PluginConfigureField> configures, PluginConfigureFieldName key)
    {
        return Long.valueOf(getStringValue(configures, key));
    }

    public static Boolean getBooleanValue(List<PluginConfigureField> configures, PluginConfigureFieldName key)
    {
        return Boolean.valueOf(getStringValue(configures, key));
    }

    public static Map<String, Object> getMapValue(List<PluginConfigureField> configures, PluginConfigureFieldName key)
    {
        Map<String, Object> values = new ConcurrentHashMap<>();
        PluginConfigureField configureField = getConfigure(configures, key);
        if (configureField.getType().equals(PluginConfigureFieldType.Array)) {
            List<LinkedHashMap<String, Object>> list = (List<LinkedHashMap<String, Object>>) configureField.getValue();
            list.forEach(map -> values.put(String.valueOf(map.get(PluginConfigureFieldName.field.name())), map.get(PluginConfigureFieldName.value.name())));
        }
        else {
            Preconditions.checkArgument(false, "Not Support type");
        }
        return values;
    }

    /**
     * Resolves the configure declared inside the plugin code for the given plugin name.
     *
     * @param pluginManager the plugin manager
     * @param type the plugin name (source type)
     * @return the plugin configure, or null when the plugin is absent or declares none
     */
    private static PluginConfigure resolvePluginConfigure(PluginManager pluginManager, String type)
    {
        return pluginManager.getPlugin(type)
                .map(Plugin::describe)
                .orElse(null);
    }

    /**
     * Converts the given properties based on the source entity, fields, configure, environment, pipeline type, executor, query, and field body.
     *
     * @param entity the source entity
     * @param environment the environment
     * @param pipelineType the pipeline type
     * @param executor the executor
     * @param query the query
     * @param fieldBody the field body
     * @return the merged properties
     */
    public static Properties convertProperties(SourceEntity entity, PluginManager pluginManager,
            PluginConfigurePipelineType pipelineType, String executor, String query, PipelineFieldBody fieldBody)
    {
        PluginConfigure yamlConfigure = resolvePluginConfigure(pluginManager, entity.getType());
        if (ObjectUtils.isEmpty(yamlConfigure) || ObjectUtils.isEmpty(yamlConfigure.getPipelines())) {
            String message = String.format("From source [ %s ] is not supported pipeline, type [ %s ]", entity.getId(), entity.getType());
            Preconditions.checkArgument(!message.isEmpty(), message);
        }

        List<PluginConfigureExecutorField> fields = Lists.newArrayList();
        Optional<PluginConfigureExecutor> configureExecutor = yamlConfigure.getPipelines()
                .stream()
                .filter(v -> v.getExecutor().equals(executor) && v.getType().equals(pipelineType))
                .findFirst();
        if (!configureExecutor.isPresent()) {
            String message = String.format("Source [ %s ] type [ %s ] is not supported pipeline type [ %s ]", entity.getId(), entity.getType(), pipelineType);
            Preconditions.checkArgument(!message.isEmpty(), message);
        }
        else {
            fields.addAll(configureExecutor.get().getFields());
            List<String> errorList = checkField(configureExecutor.get().getFields(), fieldBody.getConfigures(), entity.getName(), pipelineType.name());
            Preconditions.checkArgument(errorList.isEmpty(), String.join("\n", errorList));
        }

        Properties originProperties = fieldBody.getConfigures();
        if (pipelineType.equals(PluginConfigurePipelineType.INPUT)) {
            if (!originProperties.containsKey("context") && query != null) {
                originProperties.setProperty("context", query);
            }
        }
        return mergeProperties(entity, fields, originProperties);
    }

    public static PipelineFieldBody convertFieldBody(SourceEntity entity, String executor, PluginConfigurePipelineType pipelineType, PluginManager pluginManager, Properties originProperties)
    {
        try {
            PipelineFieldBody body = new PipelineFieldBody();
            body.setProtocol(entity.getProtocol());
            PluginConfigure yamlConfigure = resolvePluginConfigure(pluginManager, entity.getType());
            if (yamlConfigure == null) {
                throw new IllegalArgumentException("Failed to load plugin configuration");
            }

            // Pipeline 条目可能缺少 executor/type 字段（如 Doris 的 YAML 配置），
            // 直接在条目上调用 equals 会抛出 NPE，比较方向必须固定
            PluginConfigureExecutor matchedExecutor = findPipelineExecutor(yamlConfigure, executor, pipelineType);
            if (matchedExecutor != null) {
                body.setConfigures(mergeProperties(entity, matchedExecutor.getFields(), originProperties));
            }
            if (body.getConfigures() == null) {
                body.setConfigures(new Properties());
            }
            return body;
        }
        catch (Throwable e) {
            log.error("Failed to convert field body: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to convert field body: " + e.getMessage(), e);
        }
    }

    /**
     * Finds the pipeline executor matching the given executor and pipeline type.
     * Entries missing executor or type are skipped safely instead of raising NPE.
     *
     * @param yamlConfigure YAML 配置 | YAML configuration
     * @param executor 执行器 | Executor name
     * @param pipelineType 管道类型 | Pipeline type
     * @return 匹配的执行器配置，未命中时返回 null | Matched executor configure, or null
     */
    static PluginConfigureExecutor findPipelineExecutor(PluginConfigure yamlConfigure, String executor, PluginConfigurePipelineType pipelineType)
    {
        return Optional.ofNullable(yamlConfigure.getPipelines())
                .orElseGet(ArrayList::new)
                .stream()
                .filter(v -> Objects.equals(v.getExecutor(), executor) && Objects.equals(v.getType(), pipelineType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Converts options from a source entity into a set of strings.
     *
     * @param entity the source entity to convert options from
     * @param environment the environment in which the conversion is performed
     * @param executor the executor to use for the conversion
     * @param pipelineType the type of pipeline to convert options for
     * @return a set of strings representing the converted options
     */
    public static Set<String> convertOptions(SourceEntity entity, PluginManager pluginManager, String executor, PluginConfigurePipelineType pipelineType)
    {
        Set<String> fromOptions = Sets.newHashSet();
        PluginConfigure yamlConfigure = resolvePluginConfigure(pluginManager, entity.getType());
        yamlConfigure.getPipelines()
                .stream()
                .filter(v -> v.getExecutor().equals(executor) && v.getType().equals(pipelineType))
                .findFirst()
                .ifPresent(iConfigureExecutor -> iConfigureExecutor
                        .getFields()
                        .stream()
                        .filter(PluginConfigureExecutorField::isRequired)
                        .forEach(v -> fromOptions.add(v.getField())));
        return fromOptions;
    }

    /**
     * Merges the properties from the given source entity, list of fields, and configure properties.
     *
     * @param entity the source entity containing the properties to be merged
     * @param fields the list of fields to be considered for merging
     * @param configure the configure properties to be merged with the source entity properties
     * @return the merged properties
     */
    private static Properties mergeProperties(SourceEntity entity, List<PluginConfigureExecutorField> fields, Properties configure)
    {
        Properties properties = new Properties();
        Properties convertBeanProperties = BeanToPropertiesUtils.convertBeanToProperties(entity);
        for (PluginConfigureExecutorField field : fields) {
            if (field.isOverride()) {
                setProperty(field, properties, configure);
            }
            else {
                setProperty(field, properties, convertBeanProperties);
            }
        }
        return properties;
    }

    /**
     * Sets the property value for the given field.
     *
     * @param field the field to set the property value for
     * @param properties the properties object to store the property
     * @param configure the configuration properties object
     */
    private static void setProperty(PluginConfigureExecutorField field, Properties properties, Properties configure)
    {
        Object value = "None";
        if (ObjectUtils.isNotEmpty(field.getOrigin())) {
            String[] split = String.valueOf(field.getOrigin()).split("\\|");
            if (split.length > 1) {
                value = String.join(":", String.valueOf(configure.get(split[0])), String.valueOf(configure.get(split[1])));
            }
            else {
                if (ObjectUtils.isNotEmpty(configure.get(field.getOrigin()))) {
                    value = configure.get(field.getOrigin());
                }
            }
        }
        else {
            if (ObjectUtils.isNotEmpty(configure.get(field.getField()))) {
                value = configure.get(field.getField());
            }
        }
        properties.put(field.getField(), value);
    }

    /**
     * Checks the fields of the given list against the provided configuration properties.
     * If a field is required and not present in the configuration, an error message is added to the list.
     * If a field is of type SELECT and the value in the configuration is not one of the default values,
     * an error message is added to the list.
     *
     * @param fields a list of PluginConfigureExecutorField objects representing the fields to check
     * @param configures the configuration properties to check against
     * @param name the name of the field being checked
     * @param type the type of the pipeline being checked
     * @return a list of error messages for fields that do not meet the requirements
     */
    public static List<String> checkField(List<PluginConfigureExecutorField> fields, Properties configures, String name, String type)
    {
        List<String> list = Lists.newArrayList();
        fields.stream()
                .filter(PluginConfigureExecutorField::isInput)
                .forEach(field -> {
                    if (field.isRequired() && field.isInput() && field.isOverride()) {
                        if (ObjectUtils.isEmpty(configures.get(field.getField()))) {
                            list.add(String.format("The pipeline type [ %s ] of the [ %s ] field [ %s ] is a required field, please be sure to enter", type, name, field.getField()));
                        }
                    }
                    if (field.getType().equals(FieldType.SELECT)) {
                        if (!field.getDefaultValues().contains(configures.get(field.getField()))) {
                            list.add(String.format("The pipeline type [ %s ] of the [ %s ] field [ %s ] support the default value %s", type, name, field.getField(), String.join(",", field.getDefaultValues())));
                        }
                    }
                });
        return list;
    }
}
