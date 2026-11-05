package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.server.service.PluginReferences;
import org.devlive.metaforge.server.service.PluginStore;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/plugin")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class PluginController
{
    private final PluginManager pluginManager;
    private final PluginReferences references;
    private final PluginStore store;
    private final NotificationEventPublisher notifications;

    public PluginController(PluginManager pluginManager, PluginReferences references, PluginStore store, NotificationEventPublisher notifications)
    {
        this.pluginManager = pluginManager;
        this.references = references;
        this.store = store;
        this.notifications = notifications;
    }

    /** Tells the administrator how a plugin change went, once it went through. */
    private CommonResponse<Boolean> notified(CommonResponse<Boolean> response, NotificationScene scene, String name, String title, String content)
    {
        if (Boolean.TRUE.equals(response.getStatus())) {
            notifications.publish(scene, title, content, EntityType.PLUGIN, name, name);
        }
        return response;
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
        if (request.getName() == null || request.getName().isBlank()) {
            return CommonResponse.failure("Plugin package and name are required");
        }
        // 带版本的商店安装：经统计服务按配置来源下载、校验 SHA-256/大小后安装并回报下载量；
        // 仅有 url 的旧请求保持原行为
        // Store installs with a version download through the statistics service from the configured
        // source, verify SHA-256 and size, then report the download. Requests with only a url keep
        // the original behaviour.
        if (request.getVersion() != null && !request.getVersion().isBlank()) {
            PluginStore.Package target = PluginStore.Package.builder()
                    .key(request.getName())
                    .version(request.getVersion())
                    .url(request.getUrl())
                    .sha256(request.getSha256())
                    .size(request.getSize())
                    .build();
            try {
                return notified(store.install(target, (archive, name) -> pluginManager.installPlugin(archive, name))
                                ? CommonResponse.success(true) : CommonResponse.failure("Unable to install the plugin package"),
                        NotificationScene.PLUGIN_INSTALLED, request.getName(), String.format("插件 %s 已安装", request.getName()),
                        String.format("插件 %s %s 已从插件商店安装。", request.getName(), request.getVersion()));
            }
            catch (IOException e) {
                log.warn("Store install of {} {} failed: {}", request.getName(), request.getVersion(), e.getMessage());
                return CommonResponse.failure(e.getMessage());
            }
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            return CommonResponse.failure("Plugin package and name are required");
        }
        return notified(pluginManager.installPlugin(Path.of(request.getUrl()), request.getName())
                        ? CommonResponse.success(true) : CommonResponse.failure("Unable to install the plugin package"),
                NotificationScene.PLUGIN_INSTALLED, request.getName(), String.format("插件 %s 已安装", request.getName()),
                String.format("插件 %s 已安装。", request.getName()));
    }

    // 商店下载量来自统计服务，未知插件不返回（前端显示为未知而不是 0）
    // Store download counts from the statistics service; unknown plugins are absent, never zero
    @GetMapping(value = "downloads")
    public CommonResponse<Map<String, String>> downloads(@RequestParam(value = "keys", required = false, defaultValue = "") String keys)
    {
        List<String> list = Arrays.stream(keys.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .distinct()
                .limit(500)
                .collect(Collectors.toList());
        return CommonResponse.success(store.downloads(list));
    }

    @PreAuthorize(value = "hasAuthority('ADMIN')")
    @DeleteMapping(value = "uninstall/{name}")
    public CommonResponse<Boolean> uninstallPlugin(@PathVariable(value = "name") String name)
    {
        if (references.isInUse(name, pluginManager.getPluginInfos())) {
            return CommonResponse.failure("The plugin is referenced by existing resources");
        }
        return notified(pluginManager.uninstallPlugin(name) ? CommonResponse.success(true) : CommonResponse.failure("Unable to uninstall the plugin"),
                NotificationScene.PLUGIN_UNINSTALLED, name, String.format("插件 %s 已卸载", name), String.format("插件 %s 已卸载。", name));
    }

    @PreAuthorize(value = "hasAuthority('ADMIN')")
    @GetMapping(value = "references")
    public CommonResponse<Map<String, List<PluginReferences.Reference>>> listReferences()
    {
        return CommonResponse.success(references.list(pluginManager.getPluginInfos()));
    }

    // 启用/禁用同样是改变运行中 JVM 的插件生命周期，必须仅限管理员
    @PreAuthorize(value = "hasAuthority('ADMIN')")
    @PostMapping(value = "/{name}/state")
    public CommonResponse<Boolean> changeState(@PathVariable(value = "name") String name,
                                               @RequestBody PluginStateRequest request)
    {
        String state = request.isEnabled() ? "启用" : "停用";
        return notified(pluginManager.setPluginState(name, request.isEnabled()) ? CommonResponse.success(true) : CommonResponse.failure("Unable to change plugin state"),
                NotificationScene.PLUGIN_STATE_CHANGED, name, String.format("插件 %s 已%s", name, state), String.format("插件 %s 已%s。", name, state));
    }

    @Data
    public static class PluginInstallRequest
    {
        private String url;
        // 插件名称，默认作为插件的安装目录
        // Plugin name, default as the installation directory
        private String name;
        // 商店目录中的版本、SHA-256 与大小（字节）；有版本时走统计服务下载并校验
        // Version, SHA-256 and size (bytes) from the store catalogue; a version selects the verified store download
        private String version;
        private String sha256;
        private Long size;
    }

    @Data
    public static class PluginStateRequest
    {
        private boolean enabled;
    }
}
