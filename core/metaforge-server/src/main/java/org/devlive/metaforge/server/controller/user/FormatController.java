package org.devlive.metaforge.server.controller.user;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.FormatEntity;
import org.devlive.metaforge.service.service.FormatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/format")
public class FormatController
{
    private final FormatService formatService;

    public FormatController(FormatService formatService)
    {
        this.formatService = formatService;
    }

    @PostMapping(value = "sql")
    CommonResponse<String> formatterSql(@RequestBody FormatEntity configure)
    {
        return this.formatService.formatterSql(configure.getSql());
    }
}
