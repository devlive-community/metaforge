package org.devlive.metaforge.service.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.devlive.metaforge.common.response.CommonResponse;

/**
 * Signing in with GitHub, Google or an OpenID Connect provider by the authorization code flow.
 * Each provider is used only while system settings have it turned on and configured.
 */
public interface OAuthService
{
    /** Where to send the browser, and the state to keep in it when a sign-in began. */
    @Getter
    @AllArgsConstructor
    final class Redirect
    {
        private final String location;
        /** Null when no sign-in began and the location is the sign-in page with an error. */
        private final String state;
    }

    /**
     * Starts a sign-in at the provider.
     *
     * @param redirect the page to return to afterwards, a path on this site
     * @param base the address of this site as the request reached it, used when no site address is set
     */
    Redirect authorize(String provider, String redirect, String base);

    /**
     * Finishes a sign-in the provider sent back, and answers where to send the browser: the
     * callback page with a ticket for the token, or with an error code.
     *
     * @param browserState the state kept in the browser when the sign-in began
     */
    String callback(String provider, String code, String state, String browserState, String error, String base);

    /** Exchanges a ticket from {@link #callback} for the token, once. */
    CommonResponse<Object> redeem(String ticket);
}
