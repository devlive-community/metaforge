package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.service.ConfigureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/configure")
public class ConfigureController
{
    private final ConfigureService service;

    public ConfigureController(ConfigureService service)
    {
        this.service = service;
    }

    @GetMapping(value = "/executor")
    public CommonResponse<Configuration> executor()
    {
        return service.getExecutor();
    }
}
