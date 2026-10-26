package org.devlive.metaforge.server.controller;

import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.captcha.CalculateUtils;
import org.devlive.metaforge.captcha.entity.ResultEntity;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/captcha")
public class CaptchaController
{
    private final InitializerConfigure initializer;

    public CaptchaController(InitializerConfigure initializer)
    {
        this.initializer = initializer;
    }

    @GetMapping
    public CommonResponse<Object> getCaptcha(@RequestParam(value = "timestamp") Long timestamp)
    {
        if (ObjectUtils.isEmpty(timestamp)) {
            return CommonResponse.failure("timestamp is required");
        }
        // If the verification code is disabled, directly skip the logic of obtaining the verification code
        if (!initializer.getCaptchaEnable()) {
            return CommonResponse.success(Boolean.FALSE);
        }
        ResultEntity entity = CalculateUtils.generate();
        initializer.getCache().put(timestamp, entity);
        ResultEntity cleanEntity = entity.clone();
        cleanEntity.setResult(null);
        cleanEntity.setExpression(null);
        return CommonResponse.success(cleanEntity);
    }
}
