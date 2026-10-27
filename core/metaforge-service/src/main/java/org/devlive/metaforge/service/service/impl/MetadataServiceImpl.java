package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class MetadataServiceImpl
        implements MetadataService
{
    private final PluginManager pluginManager;
    private final SourceRepository repository;

    public MetadataServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    @Override
    public CommonResponse<Response> getEngines(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getEngines());
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDataTypes(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getDataTypes());
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDatabases(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getDatabases(entity.toConfigure(pluginManager, plugin)));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTables(String code, String database)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getTables(entity.toConfigure(pluginManager, plugin), database));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getColumns(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getColumns(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDatabase(String code, String database)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getDatabase(entity.toConfigure(pluginManager, plugin), database));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTable(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getTable(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTableStatement(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            TableDefinition definition = TableDefinition.create(database, table);
                            return CommonResponse.success(service.getTableStatement(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> updateAutoIncrement(String code, TableDefinition configure, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            TableDefinition definition = TableDefinition.create(database, table)
                                    .autoIncrement(configure.getAutoIncrement());
                            return CommonResponse.success(service.changeAutoIncrement(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> createTable(String code, String database, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            return CommonResponse.success(service.createTable(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> dropTable(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            TableDefinition definition = TableDefinition.create(database, table);
                            definition.setPreview(configure.isPreview());
                            return CommonResponse.success(service.dropTable(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> truncateTable(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            TableDefinition definition = TableDefinition.create(database, table);
                            definition.setPreview(configure.isPreview());
                            return CommonResponse.success(service.truncateTable(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> queryTable(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.queryTable(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> deleteData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.deleteData(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> exportData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            RuntimeConfigure conf = entity.toConfigure(pluginManager, plugin);
                            conf.setFormat(configure.getFormat());
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.queryTable(conf, configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> createColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.createColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> dropColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.dropColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.getColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> changeColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.changeColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> insertData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.insertData(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> updateData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.updateData(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getSuggests(String code, String keyword)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getSuggests(entity.toConfigure(pluginManager, plugin), keyword));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }
}
