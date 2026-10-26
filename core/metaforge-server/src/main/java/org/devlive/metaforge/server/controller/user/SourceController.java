package org.devlive.metaforge.server.controller.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.SharedSourceBody;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.SourceService;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@SuppressFBWarnings(value = {"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "EI_EXPOSE_REP2", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
@RestController()
@RequestMapping(value = "/api/v1/source")
@Slf4j
public class SourceController
{
    private final SourceService sourceService;
    private final Environment environment;

    public SourceController(SourceService sourceService, Environment environment)
    {
        this.sourceService = sourceService;
        this.environment = environment;
    }

    @PreAuthorize(value = "@userAuthorize.validateSource(#id)")
    @DeleteMapping(value = "{id}")
    public CommonResponse<Long> delete(@PathVariable(value = "id") Long id)
    {
        return this.sourceService.delete(id);
    }

    @PreAuthorize(value = "@userAuthorize.validateUser(#configure)")
    @PutMapping(value = "shared")
    public CommonResponse<Object> shared(@RequestBody SharedSourceBody configure)
    {
        return this.sourceService.shared(configure);
    }

    @SneakyThrows
    @PostMapping("uploadFile")
    public CommonResponse<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestHeader("PluginType") String pluginType)
    {
        UserEntity user = UserDetailsService.getUser();

        String cacheHome = environment.getProperty("metaforge.cache.data");
        if (StringUtils.isEmpty(cacheHome)) {
            cacheHome = String.join(File.separator, System.getProperty("user.dir"), "cache");
        }
        String userCacheHome = String.join(File.separator, cacheHome, user.getUsername(), pluginType);

        String originalFilename = file.getOriginalFilename();
        File targetFile = new File(String.join(File.separator, userCacheHome, originalFilename));
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        else {
            // If you already have cache files, clean and delete all files
            File[] files = targetFile.getParentFile().listFiles();
            for (File f : files) {
                log.info("Removing cache file {} state {}", f.getName(), f.delete());
            }
        }

        try {
            file.transferTo(targetFile);
        }
        catch (IOException e) {
            log.warn("File upload exception on user {} by type {} ", user.getUsername(), pluginType, e);
        }
        return CommonResponse.success(targetFile.getPath());
    }
}
