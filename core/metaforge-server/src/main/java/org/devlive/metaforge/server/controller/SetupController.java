package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.service.SetupInstallBody;
import org.devlive.metaforge.server.service.SetupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Anonymous first-boot endpoints powering the web setup wizard.
 */
@RestController
@RequestMapping(value = "/api/v1/setup")
public class SetupController
{
    private final SetupService setupService;

    public SetupController(SetupService setupService)
    {
        this.setupService = setupService;
    }

    @GetMapping(value = "status")
    public CommonResponse<Object> status()
    {
        Map<String, Object> result = new HashMap<>();
        result.put("installed", setupService.isInstalled());
        return CommonResponse.success(result);
    }

    @GetMapping(value = "environment")
    public CommonResponse<Object> environment()
    {
        return CommonResponse.success(setupService.environment());
    }

    @PostMapping(value = "test")
    public CommonResponse<Object> test(@RequestBody(required = false) SetupInstallBody.DatabaseConfigure configure)
    {
        return setupService.testConnection(configure);
    }

    @PostMapping(value = "install")
    public CommonResponse<Object> install(@RequestBody SetupInstallBody body)
    {
        return setupService.install(body);
    }
}
