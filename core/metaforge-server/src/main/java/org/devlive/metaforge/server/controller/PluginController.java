package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.service.common.PluginUtils;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/plugin")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class PluginController
{
    private final PluginManager pluginManager;
    private final Environment environment;

    public PluginController(PluginManager pluginManager, Environment environment)
    {
        this.pluginManager = pluginManager;
        this.environment = environment;
    }

    @GetMapping
    public CommonResponse<List<PluginMetadata>> getPlugins(@RequestParam(value = "hasConfigure", required = false) boolean hasConfigure)
    {
        List<PluginMetadata> plugins = pluginManager.getPluginInfos()
                .stream()
                .filter(v -> v.getType().equals(PluginType.CONNECTOR))
                .collect(Collectors.toList());

        if (hasConfigure) {
            plugins.forEach(plugin -> plugin.setConfigure(PluginUtils.loadYamlConfigure("JDBC", plugin.getName(), plugin.getName(), environment)));
        }

        return CommonResponse.success(pluginManager.getPluginInfos());
    }

    // 安装/卸载插件会向运行中的 JVM 加载任意外部代码，必须仅限管理员操作
    @PreAuthorize(value = "hasAuthority('ADMIN')")
    @PostMapping(value = "install")
    public CommonResponse<Boolean> installPlugin(@RequestBody PluginInstallRequest request)
    {
        return CommonResponse.success(pluginManager.installPlugin(Path.of(request.url), request.name));
    }

    @PreAuthorize(value = "hasAuthority('ADMIN')")
    @DeleteMapping(value = "uninstall/{name}")
    public CommonResponse<Boolean> uninstallPlugin(@PathVariable(value = "name") String name)
    {
        return CommonResponse.success(pluginManager.uninstallPlugin(name));
    }

    @Data
    public static class PluginInstallRequest
    {
        private String url;
        // 插件名称，默认作为插件的安装目录
        // Plugin name, default as the installation directory
        private String name;
    }
}
