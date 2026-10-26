package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@RestController
@RequestMapping("/api/v1/i18n")
public class I18nController
{
    private final Environment environment;

    public I18nController(Environment environment)
    {
        this.environment = environment;
    }

    @GetMapping("/{locale}")
    public CommonResponse<Properties> getLanguagePack(@PathVariable String locale)
    {
        try {
            String configLocation = environment.getProperty("spring.config.location");
            File i18nFile = new File(configLocation + "/i18n/messages_" + locale.replace("_", "-").toLowerCase() + ".properties");

            Properties props = new Properties();
            if (i18nFile.exists()) {
                try (InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(i18nFile), StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }

            return CommonResponse.success(props);
        }
        catch (Exception e) {
            return CommonResponse.failure(e.getMessage());
        }
    }
}
