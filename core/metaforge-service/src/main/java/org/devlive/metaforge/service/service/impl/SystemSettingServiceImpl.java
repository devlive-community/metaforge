package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.service.initializer.FsConfigure;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.FsSettings;
import org.devlive.metaforge.service.settings.MailService;
import org.devlive.metaforge.service.settings.MailSettings;
import org.devlive.metaforge.service.settings.OAuthProvider;
import org.devlive.metaforge.service.settings.OAuthSettings;
import org.devlive.metaforge.service.settings.SystemSettingSchema;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Spring beans are shared on purpose")
public class SystemSettingServiceImpl
        implements SystemSettingService
{
    private final RuntimeConfigureService configures;
    private final InitializerConfigure initializer;
    private final MailService mailService;
    private final PluginManager pluginManager;
    /** Read on every upload and query result, so kept until the settings are saved again. */
    private volatile FsSettings fs;

    public SystemSettingServiceImpl(RuntimeConfigureService configures, InitializerConfigure initializer, MailService mailService, PluginManager pluginManager)
    {
        this.configures = configures;
        this.initializer = initializer;
        this.mailService = mailService;
        this.pluginManager = pluginManager;
    }

    @Override
    public FsSettings fs()
    {
        FsSettings cached = fs;
        // A copy, so a caller can not change the settings everyone else reads.
        if (cached != null) {
            return cached.toBuilder().build();
        }
        FsConfigure defaults = initializer.getFsConfigure() == null ? new FsConfigure() : initializer.getFsConfigure();
        fs = FsSettings.from(configures.getEffective(SystemSettingSchema.CATEGORY, SystemSettingSchema.FS,
                SystemSettingSchema.fs(defaults.getType(), defaults.getAccess(), defaults.getSecret(), defaults.getEndpoint(), defaults.getBucket())));
        return fs.toBuilder().build();
    }

    /** The installed file system plugins an administrator can choose from. */
    private List<String> fsTypes()
    {
        return pluginManager.getPluginInfos().stream()
                .filter(plugin -> plugin.getType() == PluginType.FILESYSTEM)
                .map(PluginMetadata::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private FsSettings keepSecret(FsSettings fs)
    {
        if (StringUtils.isNotEmpty(fs.getSecret())) {
            return fs;
        }
        return fs.toBuilder().secret(fs().getSecret()).build();
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
    public OAuthSettings oauth(OAuthProvider provider)
    {
        return OAuthSettings.from(provider, configures.getEffective(SystemSettingSchema.CATEGORY, provider.configureName(), SystemSettingSchema.oauth(provider)));
    }

    @Override
    public Map<String, Object> settings()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auth", auth());
        result.put("mail", mail().masked());
        Map<String, OAuthSettings> oauth = new LinkedHashMap<>();
        for (OAuthProvider provider : OAuthProvider.values()) {
            oauth.put(provider.getId(), oauth(provider).masked());
        }
        result.put("oauth", oauth);
        result.put("fs", fs().masked());
        result.put("fsTypes", fsTypes());
        return result;
    }

    @Override
    public CommonResponse<Map<String, Object>> save(AuthSettings auth, MailSettings mail, Map<String, OAuthSettings> oauth, FsSettings fs)
    {
        if (auth == null || mail == null) {
            return CommonResponse.failure("The sign-in and mail settings are required");
        }
        if (StringUtils.isNotBlank(auth.getSiteUrl()) && !SystemSettingSchema.isUrl(auth.getSiteUrl())) {
            return CommonResponse.failure("The site address must start with http:// or https://");
        }
        // Checked as a whole before anything is written, so a refused provider saves nothing.
        Map<OAuthProvider, OAuthSettings> providers = new EnumMap<>(OAuthProvider.class);
        if (oauth != null) {
            for (Map.Entry<String, OAuthSettings> entry : oauth.entrySet()) {
                OAuthProvider provider = OAuthProvider.of(entry.getKey()).orElse(null);
                if (provider == null || entry.getValue() == null) {
                    continue;
                }
                OAuthSettings value = keepSecret(provider, entry.getValue());
                if (value.isEnabled() && !value.configured()) {
                    return CommonResponse.failure(provider.configurable()
                            ? "Set the client ID, the client secret and the three endpoints of " + provider.getLabel() + " before turning it on"
                            : "Set the client ID and the client secret of " + provider.getLabel() + " before turning it on");
                }
                providers.put(provider, value);
            }
        }
        FsSettings storage = fs == null ? null : keepSecret(fs);
        if (storage != null) {
            if (!fsTypes().contains(storage.getType())) {
                return CommonResponse.failure("The file system plugin " + storage.getType() + " is not installed");
            }
            if (!storage.configured()) {
                return CommonResponse.failure("Set the access key, the secret key, the endpoint and the bucket of " + storage.getType());
            }
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
        for (Map.Entry<OAuthProvider, OAuthSettings> entry : providers.entrySet()) {
            OAuthProvider provider = entry.getKey();
            CommonResponse<?> providerSaved = configures.save(SystemSettingSchema.CATEGORY, provider.configureName(), entry.getValue().toMap(provider),
                    provider.getLabel() + " sign-in");
            if (!Boolean.TRUE.equals(providerSaved.getStatus())) {
                return CommonResponse.failure(String.valueOf(providerSaved.getMessage()));
            }
        }
        if (storage != null) {
            CommonResponse<?> fsSaved = configures.save(SystemSettingSchema.CATEGORY, SystemSettingSchema.FS, storage.toMap(), "File storage");
            fs = null;
            if (!Boolean.TRUE.equals(fsSaved.getStatus())) {
                return CommonResponse.failure(String.valueOf(fsSaved.getMessage()));
            }
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

    private OAuthSettings keepSecret(OAuthProvider provider, OAuthSettings settings)
    {
        // Rebuilt from the stored form, so the fixed endpoints of GitHub and Google can not be changed.
        OAuthSettings value = OAuthSettings.from(provider, settings.toMap(provider));
        if (StringUtils.isEmpty(value.getClientSecret())) {
            value.setClientSecret(oauth(provider).getClientSecret());
        }
        return value;
    }

    private MailSettings keepPassword(MailSettings mail)
    {
        if (StringUtils.isNotEmpty(mail.getPassword())) {
            return mail;
        }
        return mail.toBuilder().password(mail().getPassword()).build();
    }
}
