package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.FsSettings;
import org.devlive.metaforge.service.settings.MailSettings;
import org.devlive.metaforge.service.settings.OAuthSettings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** System settings, for administrators only (the /api/v1/admin/** guard). */
@RestController
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@RequestMapping(value = "/api/v1/admin/settings")
public class SystemSettingController
{
    private final SystemSettingService service;

    public SystemSettingController(SystemSettingService service)
    {
        this.service = service;
    }

    @GetMapping
    public CommonResponse<Map<String, Object>> settings()
    {
        return CommonResponse.success(service.settings());
    }

    @PutMapping
    public CommonResponse<Map<String, Object>> save(@RequestBody SettingsBody body)
    {
        return service.save(body.getAuth(), body.getMail(), body.getOauth(), body.getFs());
    }

    @PostMapping(value = "mail/test")
    public CommonResponse<Boolean> testMail(@RequestBody MailTestBody body)
    {
        return service.testMail(body.getMail(), body.getTo());
    }

    @Data
    @NoArgsConstructor
    public static class SettingsBody
    {
        private AuthSettings auth;
        private MailSettings mail;
        /** Third-party sign-in providers by id; left out to keep them as they are. */
        private Map<String, OAuthSettings> oauth;
        /** File storage; left out to keep it as it is. */
        private FsSettings fs;
    }

    /** Tests the server as it is in the form, before it is saved. */
    @Data
    @NoArgsConstructor
    public static class MailTestBody
    {
        private MailSettings mail;
        private String to;
    }
}
