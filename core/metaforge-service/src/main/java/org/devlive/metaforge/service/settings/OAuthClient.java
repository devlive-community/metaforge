package org.devlive.metaforge.service.settings;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exchanges an authorization code for the signed-in account. It only calls the endpoints of the
 * provider's settings, never an address that came with the request.
 */
@Component
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class OAuthClient
{
    private static final int TIMEOUT = 10000;

    private final RestTemplate rest;

    @Autowired
    public OAuthClient()
    {
        this(template());
    }

    public OAuthClient(RestTemplate rest)
    {
        this.rest = rest;
    }

    private static RestTemplate template()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return new RestTemplate(factory);
    }

    /** The account at the provider; the email is only set when the provider says it is verified. */
    @Getter
    @AllArgsConstructor
    public static final class Profile
    {
        private final String subject;
        private final String email;
        /** A name to derive a username from: the login, the preferred username or the name. */
        private final String login;
    }

    public Profile profile(OAuthProvider provider, OAuthSettings settings, String code, String redirectUri, String verifier)
    {
        String token = token(settings, code, redirectUri, verifier);
        Map<String, Object> user = get(settings.getUserinfoUrl(), token);
        if (provider == OAuthProvider.GITHUB) {
            Object id = user.get("id");
            if (id == null) {
                throw new RestClientException("The profile has no id");
            }
            return new Profile(String.valueOf(id), githubEmail(token), text(user.get("login")));
        }
        String subject = text(user.get("sub"));
        if (subject.isEmpty()) {
            throw new RestClientException("The userinfo has no subject");
        }
        String email = verified(user.get("email_verified")) ? text(user.get("email")) : "";
        String login = StringUtils.defaultString(StringUtils.firstNonBlank(text(user.get("preferred_username")), text(user.get("name"))));
        return new Profile(subject, email, login);
    }

    private String token(OAuthSettings settings, String code, String redirectUri, String verifier)
    {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", settings.getClientId());
        form.add("client_secret", settings.getClientSecret());
        form.add("code_verifier", verifier);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = rest.postForObject(settings.getTokenUrl(), new HttpEntity<>(form, headers), Map.class);
        // GitHub answers a bad code with 200 and an error field.
        String token = body == null ? "" : text(body.get("access_token"));
        if (token.isEmpty()) {
            throw new RestClientException("No access token: " + (body == null ? "empty response" : text(body.get("error"))));
        }
        return token;
    }

    private String githubEmail(String token)
    {
        List<?> emails;
        try {
            emails = exchange(OAuthProvider.GITHUB_EMAILS, token, List.class);
        }
        catch (RestClientException e) {
            // Without the user:email scope the list is refused; the sign-in goes on without an email.
            return "";
        }
        if (emails == null) {
            return "";
        }
        for (Object item : emails) {
            if (item instanceof Map) {
                Map<?, ?> email = (Map<?, ?>) item;
                if (verified(email.get("primary")) && verified(email.get("verified"))) {
                    return text(email.get("email"));
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String url, String token)
    {
        Map<String, Object> body = exchange(url, token, Map.class);
        if (body == null) {
            throw new RestClientException("Empty userinfo response");
        }
        return body;
    }

    private <T> T exchange(String url, String token, Class<T> type)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        // GitHub refuses requests without a user agent.
        headers.set(HttpHeaders.USER_AGENT, "MetaForge");
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), type).getBody();
    }

    private static boolean verified(Object value)
    {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String text(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
