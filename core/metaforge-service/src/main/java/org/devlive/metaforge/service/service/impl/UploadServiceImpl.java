package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.common.utils.UrlUtils;
import org.devlive.metaforge.fs.FsRequest;
import org.devlive.metaforge.fs.FsResponse;
import org.devlive.metaforge.fs.FsService;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.body.UploadBody;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.enums.UploadMode;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.UploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

@Slf4j
@Service
@SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "EI_EXPOSE_REP2"})
public class UploadServiceImpl
        implements UploadService
{
    private final PluginManager pluginManager;
    private final HttpServletRequest request;
    private final InitializerConfigure initializer;

    public UploadServiceImpl(PluginManager pluginManager, HttpServletRequest request, InitializerConfigure initializer)
    {
        this.pluginManager = pluginManager;
        this.request = request;
        this.initializer = initializer;
    }

    @Override
    public CommonResponse upload(UploadBody configure)
    {
        if (configure.getMode().equals(UploadMode.DASHBOARD)) {
            configure.setCode(CodeUtils.generateCode(false));
            AvatarEntity entity = AvatarEntity.builder()
                    .type(initializer.getFsConfigure().getType())
                    .build();
            try {
                FsRequest fsRequest = getFsRequest(configure.getFile(), configure);
                pluginManager.getPlugin(initializer.getFsConfigure().getType())
                        .ifPresent(plugin -> {
                            FsService fsService = plugin.getService(FsService.class);
                            FsResponse response = fsService.writer(fsRequest);
                            entity.setPath(response.getRemote());
                            entity.setLocal(response.getRemote());
                            if (initializer.getFsConfigure().getType().startsWith("Local")) {
                                entity.setPath(getAccess(entity));
                            }
                        });
            }
            catch (IOException e) {
                log.error("Failed to upload file [ {} ]", configure.getCode(), e);
                return CommonResponse.failure(e.getMessage());
            }
            finally {
                try {
                    configure.getFile().getInputStream().close();
                }
                catch (IOException e) {
                    log.warn("Failed to close input stream", e);
                }
                return CommonResponse.success(entity);
            }
        }
        return CommonResponse.failure(String.format("Mode [ %s ] not supported", configure.getMode()));
    }

    /**
     * Generates a FsRequest object based on the given MultipartFile and UploadBody.
     *
     * @param file the MultipartFile object containing the file data
     * @param configure the UploadBody object containing the configuration
     * @return the generated FsRequest object
     * @throws IOException if there is an error reading the file input stream
     */
    private FsRequest getFsRequest(MultipartFile file, UploadBody configure)
            throws IOException
    {
        return FsRequest.builder()
                .access(initializer.getFsConfigure().getAccess())
                .secret(initializer.getFsConfigure().getSecret())
                .endpoint(getHome(configure))
                .bucket(initializer.getFsConfigure().getBucket())
                .stream(file.getInputStream())
                .fileName("avatar.png")
                .build();
    }

    /**
     * Returns the home directory path based on the given configuration.
     *
     * @param configure the upload configuration containing the mode and code
     * @return the home directory path
     */
    private String getHome(UploadBody configure)
    {
        if (!initializer.getFsConfigure().getType().startsWith("Local")) {
            return initializer.getFsConfigure().getEndpoint();
        }
        return String.join("/", initializer.getDataHome(),
                UserDetailsService.getUser().getUsername(),
                configure.getMode().toString().toLowerCase(),
                configure.getCode());
    }

    /**
     * Returns the access URL for the given AvatarEntity.
     *
     * @param configure the AvatarEntity containing the path
     * @return the access URL
     */
    private String getAccess(AvatarEntity configure)
    {
        String protocol = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        return protocol + "://" + host + ":" + port + UrlUtils.fixUrl("/upload" + configure.getPath().replaceFirst(initializer.getDataHome(), ""));
    }
}
