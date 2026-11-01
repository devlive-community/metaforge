package org.devlive.metaforge.service.settings;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;

import java.util.Arrays;
import java.util.List;

/**
 * The system settings stored in metaforge_configure under the SYSTEM category.
 * Registration and captcha default to metaforge.registration.enable and metaforge.captcha.enable;
 * every other feature is off until an administrator turns it on.
 */
public final class SystemSettingSchema
{
    public static final String CATEGORY = "SYSTEM";
    public static final String AUTH = "AUTH";
    public static final String MAIL = "MAIL";

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
                new RuntimeConfigureField("privacyUrl", RuntimeConfigureFieldType.STRING, "", "Privacy policy shown on sign-up"));
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
