package org.devlive.metaforge.server.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 启动时把每个 ExecutorPlugin 声明的可配置字段（Plugin.configures()）按默认值落到 metaforge_configure。
 * 已存在的行不动；只为缺失的 (EXECUTOR, name) 做 seed。
 *
 * On startup, iterate ExecutorPlugin instances and seed default configuration rows into
 * metaforge_configure when missing. Existing rows are left untouched.
 *
 * 在 SchedulerRunner / DatasetHistoryRecoveryRunner 之前执行（@Order=5），
 * 保证后续业务逻辑读到的是已 seed 过的配置。
 */
@Slf4j
@Service
@Order(5)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class RuntimeConfigureSeedRunner
        implements CommandLineRunner
{
    private final PluginManager pluginManager;
    private final RuntimeConfigureService runtimeConfigureService;
    private final InstallationState installationState;

    public RuntimeConfigureSeedRunner(PluginManager pluginManager, RuntimeConfigureService runtimeConfigureService, InstallationState installationState)
    {
        this.pluginManager = pluginManager;
        this.runtimeConfigureService = runtimeConfigureService;
        this.installationState = installationState;
    }

    @Override
    public void run(String... args)
    {
        if (!installationState.isInstalled()) {
            log.info("Runtime configuration seed skipped: instance is not installed");
            return;
        }
        try {
            pluginManager.getPluginInfos().stream()
                    .filter(meta -> meta.getType() == PluginType.EXECUTOR)
                    .forEach(meta -> {
                        Object instance = meta.getInstance();
                        if (!(instance instanceof Plugin)) {
                            return;
                        }
                        Plugin plugin = (Plugin) instance;
                        List<PluginConfigureField> fields = plugin.configures();
                        if (fields == null || fields.isEmpty()) {
                            return;
                        }
                        String name = plugin.getName();
                        runtimeConfigureService.seedIfAbsent(
                                RuntimeConfigureService.CATEGORY_EXECUTOR,
                                name,
                                fields,
                                "Configuration for executor " + name
                        );
                    });
        }
        catch (Exception ex) {
            log.error("Seed executor runtime configure failed", ex);
        }
    }
}
