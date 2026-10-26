package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.UploadBody;
import org.devlive.metaforge.service.service.UploadService;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "uploadController")
@RequestMapping(value = "/api/v1/upload")
public class UploadController
{
    private final UploadService service;

    public UploadController(UploadService service)
    {
        this.service = service;
    }

    @PostMapping
    public CommonResponse upload(@ModelAttribute UploadBody configure)
    {
        return service.upload(configure);
    }
}
