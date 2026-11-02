package org.devlive.metaforge.service.settings;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The system settings stored in metaforge_configure under the SYSTEM category.
 * Registration and captcha default to metaforge.registration.enable and metaforge.captcha.enable;
 * every other feature is off until an administrator turns it on. Each third-party sign-in provider
 * has a row of its own, named by {@link OAuthProvider#configureName()}.
 */
public final class SystemSettingSchema
{
    public static final String CATEGORY = "SYSTEM";
    public static final String AUTH = "AUTH";
    public static final String MAIL = "MAIL";
    public static final String FS = "FS";

    private SystemSettingSchema()
    {}

    public static List<RuntimeConfigureField> auth(boolean registration, boolean captcha)
    {
        return Arrays.asList(
                new RuntimeConfigureField("registration", RuntimeConfigureFieldType.BOOLEAN, String.valueOf(registration), "Allow new accounts to sign up"),
                new RuntimeConfigureField("captcha", RuntimeConfigureFieldType.BOOLEAN, String.valueOf(captcha), "Ask for the arithmetic captcha on sign-in and sign-up"),
                new RuntimeConfigureField("rememberMe", RuntimeConfigureFieldType.BOOLEAN, "false", "Offer to keep the sign-in for several days"),
                new RuntimeConfigureField("rememberDays", RuntimeConfigureFieldType.NUMBER, "30", "Days a remembered sign-in lasts"),
                new RuntimeConfigureField("emailSignin", RuntimeConfigureFieldType.BOOLEAN, "false", "Allow signing in with the account email"),
                new RuntimeConfigureField("emailVerification", RuntimeConfigureFieldType.BOOLEAN, "false", "Require an email and its code to sign up"),
                new RuntimeConfigureField("passwordReset", RuntimeConfigureFieldType.BOOLEAN, "false", "Allow resetting a forgotten password by email"),
                new RuntimeConfigureField("termsUrl", RuntimeConfigureFieldType.STRING, "", "Terms of service shown on sign-up"),
                new RuntimeConfigureField("privacyUrl", RuntimeConfigureFieldType.STRING, "", "Privacy policy shown on sign-up"),
                new RuntimeConfigureField("siteUrl", RuntimeConfigureFieldType.STRING, "", "Address MetaForge is opened at, for third-party sign-in callbacks"));
    }

    /** File storage; the metaforge.fs.* properties are the defaults until it is saved here. */
    public static List<RuntimeConfigureField> fs(String type, String access, String secret, String endpoint, String bucket)
    {
        return Arrays.asList(
                new RuntimeConfigureField("type", RuntimeConfigureFieldType.STRING, StringUtils.isBlank(type) ? FsSettings.LOCAL : type, "File system plugin"),
                new RuntimeConfigureField("access", RuntimeConfigureFieldType.STRING, StringUtils.defaultString(access), "Access key"),
                new RuntimeConfigureField("secret", RuntimeConfigureFieldType.PASSWORD, StringUtils.defaultString(secret), "Secret key"),
                new RuntimeConfigureField("endpoint", RuntimeConfigureFieldType.STRING, StringUtils.defaultString(endpoint), "Endpoint or domain"),
                new RuntimeConfigureField("bucket", RuntimeConfigureFieldType.STRING, StringUtils.defaultString(bucket), "Bucket"));
    }

    public static List<RuntimeConfigureField> mail()
    {
        return Arrays.asList(
                new RuntimeConfigureField("host", RuntimeConfigureFieldType.STRING, "", "SMTP host"),
                new RuntimeConfigureField("port", RuntimeConfigureFieldType.NUMBER, "465", "SMTP port"),
                new RuntimeConfigureField("ssl", RuntimeConfigureFieldType.BOOLEAN, "true", "SSL on connect; STARTTLS otherwise"),
                new RuntimeConfigureField("username", RuntimeConfigureFieldType.STRING, "", "SMTP account"),
                new RuntimeConfigureField("password", RuntimeConfigureFieldType.PASSWORD, "", "SMTP password"),
                new RuntimeConfigureField("from", RuntimeConfigureFieldType.STRING, "", "Sender address"));
    }

    public static List<RuntimeConfigureField> oauth(OAuthProvider provider)
    {
        List<RuntimeConfigureField> fields = new ArrayList<>(Arrays.asList(
                new RuntimeConfigureField("enabled", RuntimeConfigureFieldType.BOOLEAN, "false", "Offer signing in with " + provider.getLabel()),
                new RuntimeConfigureField("clientId", RuntimeConfigureFieldType.STRING, "", "OAuth client ID"),
                new RuntimeConfigureField("clientSecret", RuntimeConfigureFieldType.PASSWORD, "", "OAuth client secret")));
        if (provider.configurable()) {
            fields.addAll(Arrays.asList(
                    new RuntimeConfigureField("name", RuntimeConfigureFieldType.STRING, "", "Name shown for the provider"),
                    new RuntimeConfigureField("issuer", RuntimeConfigureFieldType.STRING, "", "Issuer"),
                    new RuntimeConfigureField("authorizationUrl", RuntimeConfigureFieldType.STRING, "", "Authorization endpoint"),
                    new RuntimeConfigureField("tokenUrl", RuntimeConfigureFieldType.STRING, "", "Token endpoint"),
                    new RuntimeConfigureField("userinfoUrl", RuntimeConfigureFieldType.STRING, "", "Userinfo endpoint"),
                    new RuntimeConfigureField("scopes", RuntimeConfigureFieldType.STRING, provider.getScopes(), "Scopes")));
        }
        return fields;
    }

    /** An absolute http or https address with a host. */
    public static boolean isUrl(String value)
    {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return (scheme.equals("http") || scheme.equals("https")) && StringUtils.isNotBlank(uri.getHost());
        }
        catch (URISyntaxException e) {
            return false;
        }
    }

    static String text(String value)
    {
        return value == null ? "" : value.trim();
    }

    static int number(String value, int fallback, int min, int max)
    {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return Math.max(min, Math.min(max, (int) Double.parseDouble(value.trim())));
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }
}
