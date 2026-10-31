package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.SourceDdlHistoryEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.repository.SourceDdlHistoryRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class MetadataServiceImpl
        implements MetadataService
{
    private final PluginManager pluginManager;
    private final SourceRepository repository;
    private final SourceDdlHistoryRepository ddlHistoryRepository;

    public MetadataServiceImpl(PluginManager pluginManager, SourceRepository repository, SourceDdlHistoryRepository ddlHistoryRepository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
        this.ddlHistoryRepository = ddlHistoryRepository;
    }

    private <T> CommonResponse<T> nativeOperation(String code, ManagementRequest request, Set<String> capabilities,
            BiFunction<PluginService, RuntimeConfigure, T> operation)
    {
        try {
            if (request == null) {
                return CommonResponse.failure("Management request is required");
            }
            request.validate();
            return repository.findByCode(code)
                    .map(entity -> pluginManager.getPlugin(entity.getType())
                            .map(plugin -> {
                                PluginService adapter = plugin.getService(PluginService.class);
                                if (capabilities.stream().noneMatch(adapter.managementCapabilities()::supports)) {
                                    return CommonResponse.<T>failure("This source does not support the requested management operation");
                                }
                                return CommonResponse.success(operation.apply(adapter, entity.toConfigure(pluginManager, plugin)));
                            })
                            .orElseGet(() -> CommonResponse.<T>failure("Source plugin not found")))
                    .orElseGet(() -> CommonResponse.failure("Source not found"));
        }
        catch (RuntimeException error) {
            return CommonResponse.failure(error.getMessage() == null ? "Management operation failed" : error.getMessage());
        }
    }

    @Override
    public CommonResponse<ManagementPage> managementNamespaces(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("namespace:list"), (adapter, configure) -> adapter.managementNamespaces(configure, request));
    }

    @Override
    public CommonResponse<ManagementPage> managementList(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("key:list", "object:list"), (adapter, configure) -> adapter.managementList(configure, request));
    }

    @Override
    public CommonResponse<ManagementDetail> managementDetail(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("key:read", "object:read"), (adapter, configure) -> adapter.managementDetail(configure, request));
    }

    @Override
    public CommonResponse<ManagementDownload> managementDownload(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("object:download"), (adapter, configure) -> adapter.managementDownload(configure, request));
    }

    @Override
    public CommonResponse<ManagementCapabilities> getCapabilities(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> CommonResponse.success(plugin.getService(PluginService.class).managementCapabilities()))
                        .orElseGet(() -> CommonResponse.<ManagementCapabilities>failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getEngines(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("engine:list")) {
                                return CommonResponse.<Response>failure("Source does not support engine:list");
                            }
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
                            if (!service.managementCapabilities().supports("datatype:list")) {
                                return CommonResponse.<Response>failure("Source does not support datatype:list");
                            }
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
                            if (!service.managementCapabilities().supports("database:list")) {
                                return CommonResponse.<Response>failure("Source does not support database:list");
                            }
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
                            if (!service.managementCapabilities().supports("table:list")) {
                                return CommonResponse.<Response>failure("Source does not support table:list");
                            }
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
                            if (!service.managementCapabilities().supports("column:read")) {
                                return CommonResponse.<Response>failure("Source does not support column:read");
                            }
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
                            if (!service.managementCapabilities().supports("database:info")) {
                                return CommonResponse.<Response>failure("Source does not support database:info");
                            }
                            return CommonResponse.success(service.getDatabase(entity.toConfigure(pluginManager, plugin), database));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDatabaseTables(String code, String database)
    {
        return withPlugin(code, List.of("database:info"), (service, configure) -> service.getDatabaseTables(configure, database));
    }

    @Override
    public CommonResponse<Response> alterDatabaseOptions(String code, String database, TableDefinition configure)
    {
        configure.setDatabase(database);
        try {
            return withPlugin(code, List.of("database:alter"), (service, runtime) -> service.alterDatabaseOptions(runtime, configure));
        }
        catch (IllegalArgumentException error) {
            return CommonResponse.failure(error.getMessage());
        }
    }

    @Override
    public CommonResponse<Response> getTable(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:info")) {
                                return CommonResponse.<Response>failure("Source does not support table:info");
                            }
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
                            if (!service.managementCapabilities().supports("table:statement")) {
                                return CommonResponse.<Response>failure("Source does not support table:statement");
                            }
                            TableDefinition definition = TableDefinition.create(database, table);
                            return CommonResponse.success(service.getTableStatement(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTableDdl(String code, String database, String table, String kind)
    {
        // Sample data reads rows; every other kind only reads the table definition.
        List<String> capabilities = "sample-data".equals(kind) ? List.of("table:statement", "table:read") : List.of("table:statement");
        try {
            return withPlugin(code, capabilities, (service, configure) -> service.getTableDdl(configure, database, table, kind));
        }
        catch (IllegalArgumentException error) {
            return CommonResponse.failure(error.getMessage());
        }
    }

    @Override
    public CommonResponse<Response> generateTableDdl(String code, String database, String table, String kind)
    {
        CommonResponse<Response> response = getTableDdl(code, database, table, kind);
        Response data = response.getData();
        if (Boolean.TRUE.equals(response.getStatus()) && data != null && Boolean.TRUE.equals(data.getIsSuccessful())) {
            UserEntity user = UserDetailsService.getUser();
            ddlHistoryRepository.save(SourceDdlHistoryEntity.builder()
                    .sourceCode(code)
                    .databaseName(database)
                    .tableName(table)
                    .kind(kind)
                    .content(data.getContent())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .createTime(new Date())
                    .build());
        }
        return response;
    }

    @Override
    public CommonResponse<PageEntity<SourceDdlHistoryEntity>> getTableDdlHistory(String code, String database, String table, int page, int size)
    {
        PageRequest request = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        return CommonResponse.success(PageEntity.build(ddlHistoryRepository.findAllBySourceCodeAndDatabaseNameAndTableNameOrderByCreateTimeDescIdDesc(code, database, table, request)));
    }

    @Override
    public CommonResponse<Response> updateAutoIncrement(String code, TableDefinition configure, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:auto-increment")) {
                                return CommonResponse.<Response>failure("Source does not support table:auto-increment");
                            }
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
                            if (!service.managementCapabilities().supports("table:create")) {
                                return CommonResponse.<Response>failure("Source does not support table:create");
                            }
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
                            if (!service.managementCapabilities().supports("table:drop")) {
                                return CommonResponse.<Response>failure("Source does not support table:drop");
                            }
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
                            if (!service.managementCapabilities().supports("table:truncate")) {
                                return CommonResponse.<Response>failure("Source does not support table:truncate");
                            }
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
                            if (!service.managementCapabilities().supports("table:read")) {
                                return CommonResponse.<Response>failure("Source does not support table:read");
                            }
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
                            if (!service.managementCapabilities().supports("data:delete")) {
                                return CommonResponse.<Response>failure("Source does not support data:delete");
                            }
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
                            if (!service.managementCapabilities().supports("table:export")) {
                                return CommonResponse.<Response>failure("Source does not support table:export");
                            }
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
                            if (!service.managementCapabilities().supports("column:create")) {
                                return CommonResponse.<Response>failure("Source does not support column:create");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.createColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> createIndex(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:create")) {
                                return CommonResponse.<Response>failure("Source does not support index:create");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.createIndex(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> previewCreateTable(String code, TableDefinition configure, String kind)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> CommonResponse.success(plugin.getService(PluginService.class)
                                .previewCreateTable(entity.toConfigure(pluginManager, plugin), configure, kind)))
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Map<String, String>> previewCreateTableBatch(String code, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService adapter = plugin.getService(PluginService.class);
                            if (!adapter.managementCapabilities().supports("table:create")) {
                                return CommonResponse.<Map<String, String>>failure("Source does not support table:create");
                            }
                            RuntimeConfigure runtime = entity.toConfigure(pluginManager, plugin);
                            Map<String, String> previews = new LinkedHashMap<>();
                            for (String kind : List.of("full", "columns", "indexes", "options")) {
                                Response response = adapter.previewCreateTable(runtime, configure, kind);
                                if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful()) || response.getContent() == null) {
                                    return CommonResponse.<Map<String, String>>failure(response == null || response.getMessage() == null
                                            ? "Failed to generate table SQL previews" : response.getMessage());
                                }
                                previews.put(kind, response.getContent());
                            }
                            return CommonResponse.success(previews);
                        })
                        .orElseGet(() -> CommonResponse.<Map<String, String>>failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> dropIndex(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:drop")) {
                                return CommonResponse.<Response>failure("Source does not support index:drop");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.dropIndex(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getIndexes(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:read")) {
                                return CommonResponse.<Response>failure("Source does not support index:read");
                            }
                            return CommonResponse.success(service.metadataIndexes(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getForeignKeys(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("foreign-key:read")) {
                                return CommonResponse.<Response>failure("Source does not support foreign-key:read");
                            }
                            return CommonResponse.success(service.metadataForeignKeys(entity.toConfigure(pluginManager, plugin), database, table));
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
                            if (!service.managementCapabilities().supports("column:drop")) {
                                return CommonResponse.<Response>failure("Source does not support column:drop");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.dropColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    /** Runs an operation on the source's plugin when it supports every listed capability. */
    private CommonResponse<Response> withPlugin(String code, List<String> capabilities, BiFunction<PluginService, RuntimeConfigure, Response> operation)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            Optional<String> missing = capabilities.stream().filter(capability -> !service.managementCapabilities().supports(capability)).findFirst();
                            if (missing.isPresent()) {
                                return CommonResponse.<Response>failure("Source does not support " + missing.get());
                            }
                            return CommonResponse.success(operation.apply(service, entity.toConfigure(pluginManager, plugin)));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getForeignKeyDetails(String code, String database, String table)
    {
        return withPlugin(code, List.of("foreign-key:read"), (service, configure) -> service.getForeignKeyDetails(configure, database, table));
    }

    @Override
    public CommonResponse<Response> createForeignKey(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        return withPlugin(code, List.of("foreign-key:create"), (service, runtime) -> service.createForeignKey(runtime, configure));
    }

    @Override
    public CommonResponse<Response> alterForeignKey(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        // Editing or rebuilding drops the foreign key and adds it again.
        return withPlugin(code, List.of("foreign-key:create", "foreign-key:drop"), (service, runtime) -> service.alterForeignKey(runtime, configure));
    }

    @Override
    public CommonResponse<Response> dropForeignKey(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        return withPlugin(code, List.of("foreign-key:drop"), (service, runtime) -> service.dropForeignKey(runtime, configure));
    }

    @Override
    public CommonResponse<Response> getTableProperties(String code, String database, String table)
    {
        return withPlugin(code, List.of("table:info"), (service, configure) -> service.getTableProperties(configure, database, table));
    }

    @Override
    public CommonResponse<Response> getCharacterSets(String code)
    {
        return withPlugin(code, List.of("table:alter"), (service, configure) -> service.getCharacterSets(configure));
    }

    @Override
    public CommonResponse<Response> alterTableOptions(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        return withPlugin(code, List.of("table:alter"), (service, runtime) -> service.alterTableOptions(runtime, configure));
    }

    @Override
    public CommonResponse<Response> getTriggerDetails(String code, String database, String table)
    {
        return withPlugin(code, List.of("trigger:read"), (service, configure) -> service.getTriggerDetails(configure, database, table));
    }

    @Override
    public CommonResponse<Response> createTrigger(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        return withPlugin(code, List.of("trigger:create"), (service, runtime) -> service.createTrigger(runtime, configure));
    }

    @Override
    public CommonResponse<Response> alterTrigger(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        if (configure.getTriggers().size() != 1) {
            return CommonResponse.failure("Change one trigger at a time");
        }
        // MySQL has no ALTER TRIGGER: editing drops the trigger and creates it again.
        return withPlugin(code, List.of("trigger:create", "trigger:drop"), (service, runtime) -> service.alterTrigger(runtime, configure));
    }

    @Override
    public CommonResponse<Response> dropTrigger(String code, String database, String table, TableDefinition configure)
    {
        configure.setDatabase(database);
        configure.setName(table);
        return withPlugin(code, List.of("trigger:drop"), (service, runtime) -> service.dropTrigger(runtime, configure));
    }

    @Override
    public CommonResponse<Response> getIndexDetails(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:read")) {
                                return CommonResponse.<Response>failure("Source does not support index:read");
                            }
                            return CommonResponse.success(service.getIndexDetails(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> alterIndex(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            // Editing or rebuilding drops the index and creates it again.
                            if (!service.managementCapabilities().supports("index:create") || !service.managementCapabilities().supports("index:drop")) {
                                return CommonResponse.<Response>failure("Source does not support index:create and index:drop");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.alterIndex(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getColumnDependencies(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            // Read-only, but only offered where columns can be dropped.
                            if (!service.managementCapabilities().supports("column:drop")) {
                                return CommonResponse.<Response>failure("Source does not support column:drop");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.getColumnDependencies(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTableDependencies(String code, String database, String table)
    {
        // Read-only, but only offered where tables can be dropped.
        return withPlugin(code, List.of("table:drop"), (service, configure) -> service.getTableDependencies(configure, database, table));
    }

    @Override
    public CommonResponse<Response> getColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("column:read")) {
                                return CommonResponse.<Response>failure("Source does not support column:read");
                            }
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
                            if (!service.managementCapabilities().supports("column:modify")) {
                                return CommonResponse.<Response>failure("Source does not support column:modify");
                            }
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
                            if (!service.managementCapabilities().supports("data:insert")) {
                                return CommonResponse.<Response>failure("Source does not support data:insert");
                            }
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
                            if (!service.managementCapabilities().supports("data:modify")) {
                                return CommonResponse.<Response>failure("Source does not support data:modify");
                            }
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
