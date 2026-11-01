package org.devlive.metaforge.service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.MailService;
import org.devlive.metaforge.service.settings.MailSettings;
import org.devlive.metaforge.service.settings.SystemSettingSchema;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class SystemSettingServiceImpl
        implements SystemSettingService
{
    private final RuntimeConfigureService configures;
    private final InitializerConfigure initializer;
    private final MailService mailService;

    public SystemSettingServiceImpl(RuntimeConfigureService configures, InitializerConfigure initializer, MailService mailService)
    {
        this.configures = configures;
        this.initializer = initializer;
        this.mailService = mailService;
    }

    @Override
    public AuthSettings auth()
    {
        return AuthSettings.from(configures.getEffective(SystemSettingSchema.CATEGORY, SystemSettingSchema.AUTH,
                SystemSettingSchema.auth(Boolean.TRUE.equals(initializer.getRegistrationEnable()), Boolean.TRUE.equals(initializer.getCaptchaEnable()))));
    }

    @Override
    public MailSettings mail()
    {
        return MailSettings.from(configures.getEffective(SystemSettingSchema.CATEGORY, SystemSettingSchema.MAIL, SystemSettingSchema.mail()));
    }

    @Override
    public Map<String, Object> settings()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auth", auth());
        result.put("mail", mail().masked());
        return result;
    }

    @Override
    public CommonResponse<Map<String, Object>> save(AuthSettings auth, MailSettings mail)
    {
        if (auth == null || mail == null) {
            return CommonResponse.failure("Both setting groups are required");
        }
        MailSettings saved = keepPassword(mail);
        CommonResponse<?> authSaved = configures.save(SystemSettingSchema.CATEGORY, SystemSettingSchema.AUTH, auth.toMap(), "Sign-in and registration");
        if (!Boolean.TRUE.equals(authSaved.getStatus())) {
            return CommonResponse.failure(String.valueOf(authSaved.getMessage()));
        }
        CommonResponse<?> mailSaved = configures.save(SystemSettingSchema.CATEGORY, SystemSettingSchema.MAIL, saved.toMap(), "Mail server");
        if (!Boolean.TRUE.equals(mailSaved.getStatus())) {
            return CommonResponse.failure(String.valueOf(mailSaved.getMessage()));
        }
        return CommonResponse.success(settings());
    }

    @Override
    public CommonResponse<Boolean> testMail(MailSettings mail, String to)
    {
        MailSettings settings = mail == null ? mail() : keepPassword(mail);
        if (!settings.ready()) {
            return CommonResponse.failure("Set the mail server and the sender address first");
        }
        if (!AuthServiceImpl.isEmail(to)) {
            return CommonResponse.failure("Enter a valid email address");
        }
        try {
            mailService.send(settings, to.trim(), "MetaForge 测试邮件 / Test mail",
                    "这是一封来自 MetaForge 的测试邮件，邮件服务配置可用。\nThis is a test mail from MetaForge; the mail server works.");
            return CommonResponse.success(Boolean.TRUE);
        }
        catch (MessagingException | RuntimeException e) {
            log.warn("Test mail to {} failed", to, e);
            return CommonResponse.failure("Failed to send mail: " + e.getMessage());
        }
    }

    private MailSettings keepPassword(MailSettings mail)
    {
        if (StringUtils.isNotEmpty(mail.getPassword())) {
            return mail;
        }
        return mail.toBuilder().password(mail().getPassword()).build();
    }
}
