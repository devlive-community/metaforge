package org.devlive.metaforge.service.settings;

import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The third-party sign-in providers. GitHub and Google have fixed endpoints; the generic OpenID
 * Connect provider ("SSO") uses the endpoints an administrator configures.
 */
@Getter
public enum OAuthProvider
{
    GITHUB("github", "GitHub", "https://github.com/login/oauth/authorize", "https://github.com/login/oauth/access_token",
            "https://api.github.com/user", "read:user user:email"),
    GOOGLE("google", "Google", "https://accounts.google.com/o/oauth2/v2/auth", "https://oauth2.googleapis.com/token",
            "https://openidconnect.googleapis.com/v1/userinfo", "openid email profile"),
    OIDC("oidc", "SSO", "", "", "", "openid email profile");

    /** GitHub leaves the email out of the profile when it is private; its verified addresses are listed here. */
    public static final String GITHUB_EMAILS = "https://api.github.com/user/emails";

    private final String id;
    private final String label;
    private final String authorizationUrl;
    private final String tokenUrl;
    private final String userinfoUrl;
    private final String scopes;

    OAuthProvider(String id, String label, String authorizationUrl, String tokenUrl, String userinfoUrl, String scopes)
    {
        this.id = id;
        this.label = label;
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl = tokenUrl;
        this.userinfoUrl = userinfoUrl;
        this.scopes = scopes;
    }

    /** Whether the administrator sets the endpoints, rather than them being fixed. */
    public boolean configurable()
    {
        return this == OIDC;
    }

    /** The name of its row in metaforge_configure under the SYSTEM category. */
    public String configureName()
    {
        return "OAUTH_" + name();
    }

    public static Optional<OAuthProvider> of(String id)
    {
        if (id == null) {
            return Optional.empty();
        }
        String value = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(provider -> provider.id.equals(value)).findFirst();
    }
}
