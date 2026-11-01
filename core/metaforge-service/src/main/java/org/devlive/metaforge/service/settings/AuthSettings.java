package org.devlive.metaforge.service.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sign-in and registration features an administrator turns on in the system settings.
 * The email features also need a configured mail server; see {@link SystemSettingSchema}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSettings
{
    private boolean registration;
    private boolean captcha;
    private boolean rememberMe;
    private int rememberDays;
    private boolean emailSignin;
    private boolean emailVerification;
    private boolean passwordReset;
    private String termsUrl;
    private String privacyUrl;
    /** Where MetaForge is opened, without a trailing slash; empty to take it from each request. */
    private String siteUrl;

    public static AuthSettings from(Map<String, String> values)
    {
        return AuthSettings.builder()
                .registration(Boolean.parseBoolean(values.get("registration")))
                .captcha(Boolean.parseBoolean(values.get("captcha")))
                .rememberMe(Boolean.parseBoolean(values.get("rememberMe")))
                .rememberDays(SystemSettingSchema.number(values.get("rememberDays"), 30, 1, 365))
                .emailSignin(Boolean.parseBoolean(values.get("emailSignin")))
                .emailVerification(Boolean.parseBoolean(values.get("emailVerification")))
                .passwordReset(Boolean.parseBoolean(values.get("passwordReset")))
                .termsUrl(SystemSettingSchema.text(values.get("termsUrl")))
                .privacyUrl(SystemSettingSchema.text(values.get("privacyUrl")))
                .siteUrl(site(values.get("siteUrl")))
                .build();
    }

    public Map<String, String> toMap()
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("registration", String.valueOf(registration));
        values.put("captcha", String.valueOf(captcha));
        values.put("rememberMe", String.valueOf(rememberMe));
        values.put("rememberDays", String.valueOf(Math.max(1, Math.min(365, rememberDays))));
        values.put("emailSignin", String.valueOf(emailSignin));
        values.put("emailVerification", String.valueOf(emailVerification));
        values.put("passwordReset", String.valueOf(passwordReset));
        values.put("termsUrl", SystemSettingSchema.text(termsUrl));
        values.put("privacyUrl", SystemSettingSchema.text(privacyUrl));
        values.put("siteUrl", site(siteUrl));
        return values;
    }

    private static String site(String value)
    {
        return StringUtils.stripEnd(SystemSettingSchema.text(value), "/");
    }
}
