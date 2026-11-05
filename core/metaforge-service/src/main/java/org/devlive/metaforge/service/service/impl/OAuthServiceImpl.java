package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserOAuthEntity;
import org.devlive.metaforge.service.repository.UserOAuthRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.service.OAuthService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.service.UserService;
import org.devlive.metaforge.service.settings.OAuthClient;
import org.devlive.metaforge.service.settings.OAuthProvider;
import org.devlive.metaforge.service.settings.OAuthSettings;
import org.devlive.metaforge.service.settings.OAuthStates;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class OAuthServiceImpl
        implements OAuthService
{
    /** The web page that finishes the sign-in; the console routes by the hash. */
    static final String CALLBACK_PAGE = "/auth/oauth";
    private static final Pattern NOT_NAME = Pattern.compile("[^A-Za-z0-9_]+");
    private static final int USERNAME_BASE = 15;

    private final SystemSettingService settings;
    private final OAuthStates states;
    private final OAuthClient client;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserOAuthRepository links;
    private final SecureRandom random = new SecureRandom();

    public OAuthServiceImpl(SystemSettingService settings, OAuthStates states, OAuthClient client, UserService userService, UserRepository userRepository,
            UserOAuthRepository links)
    {
        this.settings = settings;
        this.states = states;
        this.client = client;
        this.userService = userService;
        this.userRepository = userRepository;
        this.links = links;
    }

    /** A sign-in that stops with an error code the callback page explains. */
    private static final class Failure
            extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        private Failure(String code)
        {
            super(code);
        }
    }

    /** The address to register at the provider. */
    public static String callbackUrl(String base, OAuthProvider provider)
    {
        return base + "/api/auth/oauth/" + provider.getId() + "/callback";
    }

    private String base(String request)
    {
        String site = settings.auth().getSiteUrl();
        return StringUtils.stripEnd(StringUtils.isNotBlank(site) ? site : StringUtils.defaultString(request), "/");
    }

    /** Only a path on this site, so the sign-in can not be used to send someone elsewhere. */
    static String safeRedirect(String value)
    {
        if (StringUtils.isBlank(value) || value.length() > 2000 || !value.startsWith("/") || value.startsWith("//") || value.contains("\\")
                || value.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            return "";
        }
        return value;
    }

    private static String page(String base, String key, String value, String redirect)
    {
        Map<String, String> query = new LinkedHashMap<>();
        query.put(key, value);
        if (StringUtils.isNotEmpty(redirect)) {
            query.put("redirect", redirect);
        }
        return base + CALLBACK_PAGE + "?" + query(query);
    }

    private static String query(Map<String, String> values)
    {
        return values.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    @Override
    public Redirect authorize(String provider, String redirect, String request)
    {
        String base = base(request);
        String target = safeRedirect(redirect);
        Optional<OAuthProvider> found = OAuthProvider.of(provider);
        OAuthSettings configured = found.map(settings::oauth).orElse(null);
        if (configured == null || !configured.ready()) {
            return new Redirect(page(base, "error", "disabled", target), null);
        }
        OAuthStates.Pending pending = states.begin(found.get(), target);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("response_type", "code");
        query.put("client_id", configured.getClientId());
        query.put("redirect_uri", callbackUrl(base, found.get()));
        query.put("scope", configured.getScopes());
        query.put("state", pending.getState());
        query.put("code_challenge", OAuthStates.challenge(pending.getVerifier()));
        query.put("code_challenge_method", "S256");
        String endpoint = configured.getAuthorizationUrl();
        return new Redirect(endpoint + (endpoint.contains("?") ? "&" : "?") + query(query), pending.getState());
    }

    @Override
    public String callback(String provider, String code, String state, String browserState, String error, String request)
    {
        String base = base(request);
        OAuthStates.Pending pending = states.take(state).orElse(null);
        OAuthProvider found = OAuthProvider.of(provider).orElse(null);
        // The state must be one this site issued, for this provider, to this browser.
        if (pending == null || found == null || pending.getProvider() != found || browserState == null
                || !MessageDigest.isEqual(browserState.getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8))) {
            return page(base, "error", "state", "");
        }
        String redirect = pending.getRedirect();
        if (StringUtils.isNotBlank(error)) {
            return page(base, "error", "denied", redirect);
        }
        OAuthSettings configured = settings.oauth(found);
        if (!configured.ready()) {
            return page(base, "error", "disabled", redirect);
        }
        if (StringUtils.isBlank(code)) {
            return page(base, "error", "failed", redirect);
        }
        try {
            OAuthClient.Profile profile = client.profile(found, configured, code, callbackUrl(base, found), pending.getVerifier());
            UserEntity user = user(found, profile);
            CommonResponse<Object> issued = userService.issue(user.getUsername(), 0);
            if (!Boolean.TRUE.equals(issued.getStatus()) || issued.getData() == null) {
                return page(base, "error", "failed", redirect);
            }
            return page(base, "ticket", states.ticket(issued.getData()), redirect);
        }
        catch (Failure e) {
            return page(base, "error", e.getMessage(), redirect);
        }
        catch (RestClientException e) {
            log.warn("Signing in with {} failed: {}", found.getLabel(), e.getMessage());
            return page(base, "error", "failed", redirect);
        }
        catch (RuntimeException e) {
            log.warn("Signing in with {} failed", found.getLabel(), e);
            return page(base, "error", "failed", redirect);
        }
    }

    /** The linked user, else the user with the verified email, else a new user while registration is open. */
    private UserEntity user(OAuthProvider provider, OAuthClient.Profile profile)
    {
        Optional<UserOAuthEntity> link = links.findByProviderAndSubject(provider.getId(), profile.getSubject());
        Optional<UserEntity> linked = link.flatMap(value -> userRepository.findById(value.getUserId()));
        if (linked.isPresent()) {
            return active(linked.get());
        }
        String email = AuthServiceImpl.isEmail(profile.getEmail()) ? profile.getEmail().trim().toLowerCase(Locale.ROOT) : "";
        UserEntity user = email.isEmpty() ? null : userRepository.findByEmail(email).map(this::active).orElse(null);
        if (user == null) {
            if (!settings.auth().isRegistration()) {
                throw new Failure("registration");
            }
            user = create(provider, profile, email);
        }
        // A link left by a removed user is taken over.
        links.save(UserOAuthEntity.builder()
                .id(link.map(UserOAuthEntity::getId).orElse(null))
                .userId(user.getId())
                .provider(provider.getId())
                .subject(profile.getSubject())
                .createTime(new Date())
                .build());
        return user;
    }

    private UserEntity active(UserEntity user)
    {
        if (!user.isActive()) {
            throw new Failure("inactive");
        }
        return user;
    }

    private UserEntity create(OAuthProvider provider, OAuthClient.Profile profile, String email)
    {
        UserEntity user = new UserEntity();
        user.setUsername(username(provider, profile, email));
        // Signing in with a password needs a reset first; the provider is how this user signs in.
        user.setPassword(UUID.randomUUID().toString());
        if (!email.isEmpty()) {
            user.setEmail(email);
        }
        CommonResponse<UserEntity> saved = userService.saveOrUpdate(user);
        if (!Boolean.TRUE.equals(saved.getStatus()) || saved.getData() == null) {
            throw new Failure("failed");
        }
        return saved.getData();
    }

    /** The provider login made fit for a username, with a number added while it is taken. */
    private String username(OAuthProvider provider, OAuthClient.Profile profile, String email)
    {
        String source = StringUtils.defaultString(StringUtils.firstNonBlank(profile.getLogin(), StringUtils.substringBefore(email, "@")));
        String name = StringUtils.left(StringUtils.strip(NOT_NAME.matcher(source).replaceAll("_"), "_"), USERNAME_BASE);
        if (name.length() < 3) {
            name = provider.getId() + "_user";
        }
        if (userRepository.findByUsername(name).isEmpty()) {
            return name;
        }
        for (int i = 0; i < 20; i++) {
            String candidate = name + "_" + (1000 + random.nextInt(9000));
            if (userRepository.findByUsername(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new Failure("failed");
    }

    @Override
    public CommonResponse<Object> redeem(String ticket)
    {
        return states.redeem(ticket)
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure("The sign-in has expired; sign in again"));
    }
}
