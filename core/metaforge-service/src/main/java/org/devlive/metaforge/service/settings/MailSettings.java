package org.devlive.metaforge.service.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** The SMTP server that sends verification codes. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MailSettings
{
    private String host;
    private int port;
    private boolean ssl;
    private String username;
    private String password;
    private String from;
    /** Read only: whether a password is stored, since the password itself is never sent back. */
    private boolean passwordSet;

    public static MailSettings from(Map<String, String> values)
    {
        String password = SystemSettingSchema.text(values.get("password"));
        return MailSettings.builder()
                .host(SystemSettingSchema.text(values.get("host")))
                .port(SystemSettingSchema.number(values.get("port"), 465, 1, 65535))
                .ssl(Boolean.parseBoolean(values.get("ssl")))
                .username(SystemSettingSchema.text(values.get("username")))
                .password(password)
                .from(SystemSettingSchema.text(values.get("from")))
                .passwordSet(!password.isEmpty())
                .build();
    }

    public Map<String, String> toMap()
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("host", SystemSettingSchema.text(host));
        values.put("port", String.valueOf(Math.max(1, Math.min(65535, port))));
        values.put("ssl", String.valueOf(ssl));
        values.put("username", SystemSettingSchema.text(username));
        values.put("password", SystemSettingSchema.text(password));
        values.put("from", SystemSettingSchema.text(from));
        return values;
    }

    /** Mail can be sent once a server and a sender address are set. */
    public boolean ready()
    {
        return StringUtils.isNotBlank(host) && StringUtils.isNotBlank(from);
    }

    /** The settings as the administrator sees them: everything except the password. */
    public MailSettings masked()
    {
        return toBuilder().password("").passwordSet(StringUtils.isNotEmpty(password)).build();
    }
}
