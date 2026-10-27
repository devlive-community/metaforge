package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
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

@RestController
@RequestMapping(value = "/api/v1/plugin")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class PluginController
{
    private final PluginManager pluginManager;

    public PluginController(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    @GetMapping
    public CommonResponse<List<PluginMetadata>> getPlugins(@RequestParam(value = "hasConfigure", required = false) boolean hasConfigure)
    {
        // Configure 由插件代码内声明，PluginManager 加载时已填充到 PluginMetadata
        // Configure is declared in plugin code and populated into PluginMetadata by PluginManager at load time
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
