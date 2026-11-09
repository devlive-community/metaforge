package org.devlive.metaforge.service.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One third-party sign-in provider. It is offered on the sign-in page only once it is turned on
 * and fully configured; the endpoints and scopes of GitHub and Google are fixed.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OAuthSettings
{
    private boolean enabled;
    private String clientId;
    private String clientSecret;
    /** Read only: whether a secret is stored, since the secret itself is never sent back. */
    private boolean secretSet;
    /** The button label of the OpenID Connect provider; "SSO" when empty. */
    private String name;
    private String issuer;
    private String authorizationUrl;
    private String tokenUrl;
    private String userinfoUrl;
    private String scopes;

    public static OAuthSettings from(OAuthProvider provider, Map<String, String> values)
    {
        String secret = SystemSettingSchema.text(values.get("clientSecret"));
        OAuthSettings settings = OAuthSettings.builder()
                .enabled(Boolean.parseBoolean(values.get("enabled")))
                .clientId(SystemSettingSchema.text(values.get("clientId")))
                .clientSecret(secret)
                .secretSet(!secret.isEmpty())
                .name("")
                .issuer("")
                .authorizationUrl(provider.getAuthorizationUrl())
                .tokenUrl(provider.getTokenUrl())
                .userinfoUrl(provider.getUserinfoUrl())
                .scopes(provider.getScopes())
                .build();
        if (provider.configurable()) {
            settings.setName(SystemSettingSchema.text(values.get("name")));
            settings.setIssuer(SystemSettingSchema.text(values.get("issuer")));
            settings.setAuthorizationUrl(SystemSettingSchema.text(values.get("authorizationUrl")));
            settings.setTokenUrl(SystemSettingSchema.text(values.get("tokenUrl")));
            settings.setUserinfoUrl(SystemSettingSchema.text(values.get("userinfoUrl")));
            settings.setScopes(StringUtils.defaultIfBlank(SystemSettingSchema.text(values.get("scopes")), provider.getScopes()));
        }
        return settings;
    }

    /** The stored values; the fixed endpoints of GitHub and Google are not stored. */
    public Map<String, String> toMap(OAuthProvider provider)
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("enabled", String.valueOf(enabled));
        values.put("clientId", SystemSettingSchema.text(clientId));
        values.put("clientSecret", SystemSettingSchema.text(clientSecret));
        if (provider.configurable()) {
            values.put("name", SystemSettingSchema.text(name));
            values.put("issuer", SystemSettingSchema.text(issuer));
            values.put("authorizationUrl", SystemSettingSchema.text(authorizationUrl));
            values.put("tokenUrl", SystemSettingSchema.text(tokenUrl));
            values.put("userinfoUrl", SystemSettingSchema.text(userinfoUrl));
            values.put("scopes", StringUtils.defaultIfBlank(SystemSettingSchema.text(scopes), provider.getScopes()));
        }
        return values;
    }

    /** Everything a sign-in needs is set: the client, and for OpenID Connect its three endpoints. */
    public boolean configured()
    {
        return StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(clientSecret)
                && SystemSettingSchema.isUrl(authorizationUrl) && SystemSettingSchema.isUrl(tokenUrl) && SystemSettingSchema.isUrl(userinfoUrl);
    }

    /** Offered on the sign-in page. */
    public boolean ready()
    {
        return enabled && configured();
    }

    /** The settings as the administrator sees them: everything except the secret. */
    public OAuthSettings masked()
    {
        return toBuilder().clientSecret("").secretSet(StringUtils.isNotEmpty(clientSecret)).build();
    }
}
