package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.service.entity.ConfigureEntity;
import org.devlive.metaforge.service.repository.ConfigureRepository;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class RuntimeConfigureServiceImpl
        implements RuntimeConfigureService
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<Map<String, String>>() {};

    private final ConfigureRepository repository;

    public RuntimeConfigureServiceImpl(ConfigureRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public Map<String, String> getEffective(String category, String name, List<RuntimeConfigureField> fields)
    {
        Map<String, String> result = new LinkedHashMap<>();
        // 先 fill 默认值，保证调用方拿到的 map 字段齐全
        if (fields != null) {
            for (RuntimeConfigureField field : fields) {
                if (field.getDefaultValue() != null) {
                    result.put(field.getName(), field.getDefaultValue());
                }
            }
        }
        // 再 overlay DB 行
        result.putAll(getRaw(category, name));
        return result;
    }

    @Override
    public Map<String, String> getRaw(String category, String name)
    {
        return repository.findByCategoryAndName(category, name)
                .map(entity -> parseJson(entity.getConfigure()))
                .orElseGet(Collections::emptyMap);
    }

    @Override
    public List<ConfigureEntity> list(String category)
    {
        return repository.findAllByCategoryOrderByName(category);
    }

    @Override
    public CommonResponse<ConfigureEntity> save(String category, String name, Map<String, String> configMap, String description)
    {
        try {
            Optional<ConfigureEntity> existing = repository.findByCategoryAndName(category, name);
            ConfigureEntity entity = existing.orElseGet(ConfigureEntity::new);
            entity.setCategory(category);
            entity.setName(name);
            entity.setConfigure(MAPPER.writeValueAsString(configMap == null ? Collections.emptyMap() : configMap));
            if (description != null) {
                entity.setDescription(description);
            }
            entity.setUpdateTime(new Date());
            ConfigureEntity saved = repository.save(entity);
            return CommonResponse.success(saved);
        }
        catch (Exception ex) {
            log.error("Save runtime configure failed: category={} name={}", category, name, ex);
            return CommonResponse.failure("Save configure failed: " + ex.getMessage());
        }
    }

    @Override
    public void seedIfAbsent(String category, String name, List<RuntimeConfigureField> fields, String description)
    {
        if (repository.existsByCategoryAndName(category, name)) {
            return;
        }
        Map<String, String> defaults = new LinkedHashMap<>();
        if (fields != null) {
            for (RuntimeConfigureField field : fields) {
                if (field.getDefaultValue() != null) {
                    defaults.put(field.getName(), field.getDefaultValue());
                }
            }
        }
        CommonResponse<ConfigureEntity> response = save(category, name, defaults, description);
        if (Boolean.TRUE.equals(response.getStatus())) {
            log.info("Seeded runtime configure: category={} name={} fields={}", category, name, defaults.size());
        }
    }

    private Map<String, String> parseJson(String json)
    {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            Map<String, String> parsed = MAPPER.readValue(json, MAP_TYPE);
            return parsed == null ? new HashMap<>() : parsed;
        }
        catch (Exception ex) {
            log.warn("Parse runtime configure JSON failed: {}", ex.getMessage());
            return new HashMap<>();
        }
    }
}
