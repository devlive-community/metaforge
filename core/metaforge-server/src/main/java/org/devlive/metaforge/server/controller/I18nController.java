package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@RestController
@RequestMapping("/api/v1/i18n")
public class I18nController
{
    private final Environment environment;
    private final ApplicationArguments applicationArguments;
    private final ResourceLoader resourceLoader;

    public I18nController(Environment environment, ApplicationArguments applicationArguments, ResourceLoader resourceLoader)
    {
        this.environment = environment;
        this.applicationArguments = applicationArguments;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/{locale}")
    public CommonResponse<Properties> getLanguagePack(@PathVariable String locale)
    {
        try {
            String normalized = locale.replace('_', '-').toLowerCase(Locale.ROOT);
            if (normalized.equals("zh")) {
                normalized = "zh-cn";
            }
            if (!normalized.equals("zh-cn") && !normalized.equals("en")) {
                return CommonResponse.failure("Unsupported locale: " + locale);
            }
            String filename = "messages_" + normalized + ".properties";
            Properties props = new Properties();
            load(new ClassPathResource("i18n/" + filename), props);
            // Explicit external translations override the bundled defaults.
            for (String location : configurationLocations()) {
                Resource resource = externalResource(location, filename);
                if (resource.exists()) {
                    load(resource, props);
                    break;
                }
            }
            if (props.isEmpty()) {
                return CommonResponse.failure("Language pack not found: " + normalized);
            }
            return CommonResponse.success(props);
        }
        catch (Exception e) {
            return CommonResponse.failure(e.getMessage());
        }
    }

    private List<String> configurationLocations()
    {
        List<String> locations = new ArrayList<>();
        for (String property : new String[] {"spring.config.additional-location", "spring.config.location"}) {
            List<String> values = applicationArguments.getOptionValues(property);
            String value = values == null ? environment.getProperty(property) : String.join(",", values);
            if (value != null) {
                List<String> candidates = new ArrayList<>();
                Collections.addAll(candidates, value.split("[,;]"));
                Collections.reverse(candidates);
                locations.addAll(candidates);
            }
        }
        return locations;
    }

    private Resource externalResource(String location, String filename)
    {
        String base = location.trim();
        if (base.startsWith("optional:")) {
            base = base.substring("optional:".length());
        }
        if (!base.startsWith("file:") && !base.startsWith("classpath:")) {
            base = new File(base).toURI().toString();
        }
        if (base.endsWith(".properties") || base.endsWith(".yaml") || base.endsWith(".yml")) {
            base = base.substring(0, base.lastIndexOf('/') + 1);
        }
        if (!base.endsWith("/")) {
            base += "/";
        }
        return resourceLoader.getResource(base + "i18n/" + filename);
    }

    private void load(Resource resource, Properties properties)
            throws Exception
    {
        if (resource.exists()) {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
    }
}
