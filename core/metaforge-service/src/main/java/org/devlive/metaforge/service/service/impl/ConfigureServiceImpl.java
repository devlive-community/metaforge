package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.itransient.configuration.CategoryConfiguration;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.itransient.configuration.NodeConfiguration;
import org.devlive.metaforge.service.service.ConfigureService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
public class ConfigureServiceImpl
        implements ConfigureService
{
    private final Environment environment;

    public ConfigureServiceImpl(Environment environment)
    {
        this.environment = environment;
    }

    @Override
    public CommonResponse<Configuration> getExecutor()
    {
        try {
            Configuration config = new Configuration();
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Path baseDir = Paths.get(Path.of(requireNonNull(environment.getProperty("spring.config.location")), "executor").toUri());

            Path categoryPath = baseDir.resolve("category.yaml");
            if (Files.exists(categoryPath)) {
                List<CategoryConfiguration> categories = mapper.readValue(
                        categoryPath.toFile(),
                        mapper.getTypeFactory().constructCollectionType(List.class, CategoryConfiguration.class)
                );
                config.setCategories(categories);
            }

            try (Stream<Path> files = Files.list(baseDir)) {
                List<NodeConfiguration> nodes = files
                        .filter(path -> path.toString().endsWith(".yaml"))
                        .filter(path -> !path.getFileName().toString().equals("category.yaml"))
                        .map(path -> {
                            try {
                                return mapper.readValue(path.toFile(), NodeConfiguration.class);
                            }
                            catch (IOException e) {
                                log.debug("Failed to read node configuration", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                config.setNodes(nodes);
            }

            return CommonResponse.success(config);
        }
        catch (IOException e) {
            log.warn("Failed to get executor configuration", e);
            return CommonResponse.failure("Failed to get executor configuration");
        }
    }
}
