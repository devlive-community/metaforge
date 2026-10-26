package org.devlive.metaforge.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.common.utils.ResourceUtils;
import org.devlive.metaforge.service.configure.IConfigure;
import org.devlive.metaforge.spi.PluginService;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class PluginUtils
{
    private static final ObjectMapper yamlFactory = new ObjectMapper(new YAMLFactory());

    private PluginUtils() {}

    public static Optional<PluginService> getPluginByName(Injector injector, String pluginName)
    {
        return injector.getInstance(Key.get(new TypeLiteral<Set<PluginService>>() {}))
                .stream()
                .filter(plugin -> plugin.name().equalsIgnoreCase(pluginName))
                .findFirst();
    }

    public static Optional<PluginService> getPluginByNameAndType(Injector injector, String pluginName, String pluginType)
    {
        return injector.getInstance(Key.get(new TypeLiteral<Set<PluginService>>() {}))
                .stream()
                .filter(plugin -> plugin.name().equalsIgnoreCase(pluginName) && plugin.type().name().equalsIgnoreCase(pluginType))
                .findFirst();
    }

    @Deprecated
    public static IConfigure loadConfigure(String type, String plugin, String resource, Environment environment)
    {
        String root = environment.getProperty("spring.config.location");
        if (!resource.endsWith(".json")) {
            resource = resource + ".json";
        }
        String path = root + String.format("plugins/%s/%s", type.toLowerCase(), resource.toLowerCase());
        log.info("Load plugin {} type {} resource {} configure file path {}", plugin, type, resource, path);
        String json = ResourceUtils.loadResourceToString(path, true);
        if (StringUtils.isEmpty(json)) {
            log.warn("Plugin {} type {} configuration file {} not found, load default configuration file", plugin, type, resource);
            json = ResourceUtils.loadResourceToString(root + "plugins/default.json", true);
        }
        Preconditions.checkArgument(StringUtils.isNotEmpty(json), "Invalid configuration file, it may be a bug, please submit issues to solve it.");
        IConfigure configure = null;
        try {
            configure = JsonUtils.objectmapper.readValue(json, IConfigure.class);
        }
        catch (JsonProcessingException e) {
            log.error("Format configuration file, it may be a bug, please submit issues to solve it. plugin {} type {} resource {} configure file path {} message ", plugin, type, resource, path, e);
            Preconditions.checkArgument(StringUtils.isNotEmpty(json), "Format configuration file, it may be a bug, please submit issues to solve it.");
        }
        return configure;
    }

    public static IConfigure loadYamlConfigure(String type, String plugin, String resource, Environment environment)
    {
        String root = environment.getProperty("spring.config.location");
        if (resource.contains(" ")) {
            resource = resource.split(" ")[0];
        }
        if (!resource.endsWith(".yaml")) {
            resource = resource + ".yaml";
        }
        String path = root + String.format("plugins/%s", resource.toLowerCase());
        log.info("Load plugin {} type {} resource {} configure file path {}", plugin, type, resource, path);
        File file = new File(path);
        if (!file.exists()) {
            log.warn("Plugin {} type {} configuration file {} not found, load default configuration file", plugin, type, resource);
            file = new File(root + "plugins/default.yaml");
        }
        else {
            log.info("Load plugin {} type {} resource {} configure file path {}", plugin, type, resource, path);
        }

        IConfigure configure = null;
        try {
            yamlFactory.findAndRegisterModules();
            configure = yamlFactory.readValue(file, IConfigure.class);
        }
        catch (Throwable e) {
            log.error("Format configuration file, it may be a bug, please submit issues to solve it. plugin {} type {} resource {} configure file path {} message ", plugin, type, resource, path, e);
            Preconditions.checkArgument(StringUtils.isNotEmpty(path), "Format configuration file, it may be a bug, please submit issues to solve it.");
        }
        return configure;
    }
}
