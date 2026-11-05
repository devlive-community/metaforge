package org.devlive.metaforge.server.controller.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.notify.UserNotifySettings;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The signed-in user's own notification channels, their settings and subscribed scenes. */
@RestController
@RequestMapping(value = "/api/v1/user/notify")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The settings service is a Spring-managed singleton")
public class NotifySettingsController
{
    private final UserNotifySettings settings;

    public NotifySettingsController(UserNotifySettings settings)
    {
        this.settings = settings;
    }

    @GetMapping(value = "catalog")
    public CommonResponse<UserNotifySettings.Catalog> catalog()
    {
        return CommonResponse.success(settings.catalog(UserDetailsService.getUser()));
    }

    @GetMapping
    public CommonResponse<List<UserNotifySettings.ChannelSetting>> settings()
    {
        return settings.settings(UserDetailsService.getUser());
    }

    @PutMapping
    public CommonResponse<List<UserNotifySettings.ChannelSetting>> save(@RequestBody List<UserNotifySettings.ChannelSetting> configure)
    {
        return settings.save(UserDetailsService.getUser(), configure);
    }

    @PostMapping(value = "test")
    public CommonResponse<String> test(@RequestBody UserNotifySettings.ChannelSetting configure)
    {
        return settings.test(UserDetailsService.getUser(), configure);
    }
}
