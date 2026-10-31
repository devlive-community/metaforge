package org.devlive.metaforge.service.service.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.FunctionType;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FunctionUrlPreviewBody;
import org.devlive.metaforge.service.body.FunctionsImportBody;
import org.devlive.metaforge.service.entity.FunctionEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.FunctionsRepository;
import org.devlive.metaforge.service.service.FunctionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FunctionServiceImpl
        implements FunctionService
{
    // Recognized knowledge keys; every other entry key lands in the attributes JSON bag.
    private static final List<String> KNOWN_KEYS = List.of("name", "type", "category", "description", "syntax",
            "examples", "parameters", "return_type", "returntype", "notes", "version");

    private final FunctionsRepository functionsRepository;

    public FunctionServiceImpl(FunctionsRepository functionsRepository)
    {
        this.functionsRepository = functionsRepository;
    }

    @Override
    public CommonResponse<PageEntity<FunctionEntity>> getAllByFilter(FilterBody filter)
    {
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(this.functionsRepository.findAll(pageable)));
    }

    @Override
    public CommonResponse<FunctionEntity> getById(Long id)
    {
        return this.functionsRepository.findById(id)
                .map(CommonResponse::success)
                .orElse(CommonResponse.failure(String.format("Function [ %s ] not found", id)));
    }

    @Override
    public CommonResponse<Object> batchImport(FunctionsImportBody configure)
    {
        boolean overwrite = "overwrite".equalsIgnoreCase(configure.getDuplicate());
        Map<String, FunctionEntity> existing = new LinkedHashMap<>();
        for (FunctionEntity entity : this.functionsRepository.findAll()) {
            existing.putIfAbsent(entity.getName(), entity);
        }

        int imported = 0;
        int duplicated = 0;
        int invalid = 0;
        List<FunctionEntity> pending = new ArrayList<>();

        for (Map<String, String> entry : configure.getEntries()) {
            String name = normalize(entry.get("name"));
            if (StringUtils.isEmpty(name)) {
                invalid++;
                continue;
            }
            FunctionEntity function = new FunctionEntity();
            function.setName(name);
            function.setPlugin(configure.getPlugin());
            function.setType(resolveType(entry.get("type"), configure.getType()));
            function.setCategory(normalize(entry.get("category")));
            function.setDescription(normalize(entry.get("description")));
            function.setSyntax(normalize(entry.get("syntax")));
            function.setExamples(normalize(entry.get("examples")));
            function.setParameters(normalize(entry.get("parameters")));
            String returnType = normalize(entry.get("return_type"));
            if (returnType == null) {
                returnType = normalize(entry.get("returntype"));
            }
            function.setReturnType(returnType);
            function.setNotes(normalize(entry.get("notes")));
            function.setVersion(normalize(entry.get("version")));

            Map<String, String> attributes = new LinkedHashMap<>();
            entry.forEach((key, value) -> {
                String normalized = key == null ? "" : key.trim().toLowerCase().replace("-", "_");
                if (!KNOWN_KEYS.contains(normalized) && StringUtils.isNotEmpty(value)) {
                    attributes.put(key.trim(), value);
                }
            });
            if (!attributes.isEmpty()) {
                function.setAttributes(JsonUtils.toJSON(attributes));
            }

            // keep the completion index columns fed from the knowledge fields
            syncCompletionColumns(function);

            FunctionEntity target = existing.get(name);
            if (target != null && !overwrite) {
                duplicated++;
                continue;
            }
            if (target != null) {
                function.setId(target.getId());
                function.setCode(target.getCode());
            }
            pending.add(function);
            imported++;
        }

        if (!configure.isContinueOnError() && invalid > 0) {
            return CommonResponse.failure(String.format("%d entries are invalid; import aborted", invalid));
        }
        this.functionsRepository.saveAll(pending);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", configure.getEntries().size());
        result.put("imported", imported);
        result.put("duplicated", duplicated);
        result.put("invalid", invalid);
        return CommonResponse.success(result);
    }

    @Override
    public CommonResponse<String> previewUrl(FunctionUrlPreviewBody configure)
    {
        try {
            return CommonResponse.success(IOUtils.toString(new URI(configure.getUrl().trim()), "UTF-8"));
        }
        catch (URISyntaxException | IOException e) {
            return CommonResponse.failure(ServiceState.INVALID_REMOTE_ADDRESS, ServiceState.INVALID_REMOTE_ADDRESS.getValue() + ":" + e.getMessage());
        }
    }

    @Override
    public CommonResponse<FunctionEntity> saveOrUpdate(BaseRepository<FunctionEntity, Long> repository, FunctionEntity configure)
    {
        syncCompletionColumns(configure);
        return FunctionService.super.saveOrUpdate(repository, configure);
    }

    @Override
    public CommonResponse<PageEntity<FunctionEntity>> getAllByPlugin(String plugin)
    {
        FilterBody filter = new FilterBody();
        filter.setSize(Integer.MAX_VALUE);
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(this.functionsRepository.findAllByPluginContaining(pageable, plugin)));
    }

    private void syncCompletionColumns(FunctionEntity function)
    {
        // the editor completion still reads content/example; keep them derived from the knowledge fields
        if (StringUtils.isNotEmpty(function.getSyntax())) {
            function.setContent(function.getSyntax());
        }
        if (StringUtils.isNotEmpty(function.getExamples())) {
            function.setExample(function.getExamples().split("\\r?\\n")[0]);
        }
    }

    private FunctionType resolveType(String value, FunctionType fallback)
    {
        if (value == null) {
            return fallback;
        }
        try {
            return FunctionType.valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String normalize(String value)
    {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
