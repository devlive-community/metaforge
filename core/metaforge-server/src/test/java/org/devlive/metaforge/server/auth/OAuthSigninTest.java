package org.devlive.metaforge.server.auth;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserOAuthEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.UserOAuthRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.service.OAuthService;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.service.UserService;
import org.devlive.metaforge.service.service.impl.AuthServiceImpl;
import org.devlive.metaforge.service.service.impl.OAuthServiceImpl;
import org.devlive.metaforge.service.service.impl.SystemSettingServiceImpl;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.EmailCodes;
import org.devlive.metaforge.service.settings.MailService;
import org.devlive.metaforge.service.settings.MailSettings;
import org.devlive.metaforge.service.settings.OAuthClient;
import org.devlive.metaforge.service.settings.OAuthProvider;
import org.devlive.metaforge.service.settings.OAuthSettings;
import org.devlive.metaforge.service.settings.OAuthStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OAuthSigninTest
{
    private static final String SITE = "https://metaforge.example.com";
    private static final OAuthSettings GITHUB = OAuthSettings.from(OAuthProvider.GITHUB,
            Map.of("enabled", "true", "clientId", "gh-client", "clientSecret", "gh-secret"));
    private static final OAuthSettings OIDC = OAuthSettings.from(OAuthProvider.OIDC, Map.of("enabled", "true", "clientId", "sso-client",
            "clientSecret", "sso-secret", "name", "Okta", "authorizationUrl", "https://sso.example.com/authorize", "tokenUrl", "https://sso.example.com/token",
            "userinfoUrl", "https://sso.example.com/userinfo"));

    private SystemSettingService settings;
    private UserService users;
    private UserRepository repository;
    private UserOAuthRepository links;
    private MockRestServiceServer server;
    private OAuthServiceImpl service;
    private final Map<OAuthProvider, OAuthSettings> providers = new HashMap<>();

    @BeforeEach
    public void setUp()
    {
        settings = mock(SystemSettingService.class);
        users = mock(UserService.class);
        repository = mock(UserRepository.class);
        links = mock(UserOAuthRepository.class);
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        when(settings.auth()).thenReturn(AuthSettings.builder().registration(true).siteUrl(SITE).build());
        when(settings.mail()).thenReturn(new MailSettings());
        when(settings.oauth(any())).thenAnswer(invocation -> providers.getOrDefault(invocation.getArgument(0), new OAuthSettings()));
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(links.findByProviderAndSubject(anyString(), anyString())).thenReturn(Optional.empty());
        when(users.issue(anyString(), anyLong())).thenAnswer(invocation -> CommonResponse.success("jwt:" + invocation.getArgument(0)));
        service = new OAuthServiceImpl(settings, new OAuthStates(), new OAuthClient(rest), users, repository, links);
    }

    private static UserEntity user(long id, String username, String email)
    {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    /** The query of a location, including one after the hash of the callback page. */
    private static Map<String, String> query(String location)
    {
        Map<String, String> values = new HashMap<>();
        String query = location.substring(location.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return values;
    }

    private void githubAnswers(String emails)
    {
        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of("code", "the-code", "client_id", "gh-client", "client_secret", "gh-secret",
                        "redirect_uri", SITE + "/api/auth/oauth/github/callback")))
                .andRespond(withSuccess("{\"access_token\":\"gh-token\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andExpect(header("Authorization", "Bearer gh-token"))
                .andRespond(withSuccess("{\"id\":42,\"login\":\"octo-cat\",\"email\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(OAuthProvider.GITHUB_EMAILS))
                .andRespond(withSuccess(emails, MediaType.APPLICATION_JSON));
    }

    /** Starts a sign-in and comes back with the code, as the provider would. */
    private Map<String, String> signIn(String provider, String redirect)
    {
        OAuthService.Redirect started = service.authorize(provider, redirect, "http://ignored");
        String state = query(started.getLocation()).get("state");
        return query(service.callback(provider, "the-code", state, started.getState(), null, "http://ignored"));
    }

    @Test
    public void onlyProvidersThatAreOnAndConfiguredAreOffered()
    {
        AuthServiceImpl auth = new AuthServiceImpl(settings, new EmailCodes(), mock(MailService.class), users, repository, mock(PasswordEncoder.class),
                mock(InitializerConfigure.class));
        assertEquals(List.of(), auth.options().get("providers"));

        providers.put(OAuthProvider.GITHUB, GITHUB);
        providers.put(OAuthProvider.GOOGLE, GITHUB.toBuilder().enabled(false).build());
        // Turned on, but without its endpoints.
        providers.put(OAuthProvider.OIDC, OIDC.toBuilder().tokenUrl("").build());
        assertEquals(List.of(Map.of("id", "github", "name", "GitHub")), auth.options().get("providers"));

        providers.put(OAuthProvider.OIDC, OIDC);
        assertEquals(List.of(Map.of("id", "github", "name", "GitHub"), Map.of("id", "oidc", "name", "Okta")), auth.options().get("providers"));
    }

    @Test
    public void aProviderThatIsOffGoesBackToTheSignInPage()
    {
        OAuthService.Redirect started = service.authorize("github", "/query", null);
        assertNull(started.getState());
        assertEquals(SITE + "/#/auth/oauth?error=disabled&redirect=%2Fquery", started.getLocation());
        assertNull(service.authorize("unknown", null, null).getState());
    }

    @Test
    public void theAuthorizationRequestUsesTheSiteAddressStateAndPkce()
    {
        providers.put(OAuthProvider.GITHUB, GITHUB);
        OAuthService.Redirect started = service.authorize("github", "//evil.example.com", "http://internal:9096");
        assertTrue(started.getLocation().startsWith("https://github.com/login/oauth/authorize?"));
        Map<String, String> query = query(started.getLocation());
        assertEquals("gh-client", query.get("client_id"));
        assertEquals(SITE + "/api/auth/oauth/github/callback", query.get("redirect_uri"));
        assertEquals("read:user user:email", query.get("scope"));
        assertEquals(started.getState(), query.get("state"));
        assertEquals("S256", query.get("code_challenge_method"));
        assertEquals(43, query.get("code_challenge").length());

        // Without a site address the request's own address is used; an outside redirect is dropped.
        when(settings.auth()).thenReturn(AuthSettings.builder().build());
        started = service.authorize("github", "//evil.example.com", "http://internal:9096/");
        assertEquals("http://internal:9096/api/auth/oauth/github/callback", query(started.getLocation()).get("redirect_uri"));
        // A state started for one provider is refused by another.
        String state = started.getState();
        assertEquals("http://internal:9096/#/auth/oauth?error=state", service.callback("google", "the-code", state, state, null, "http://internal:9096"));
    }

    @Test
    public void theStateMustComeBackOnceToTheBrowserThatStarted()
    {
        providers.put(OAuthProvider.GITHUB, GITHUB);
        String state = service.authorize("github", "/query", null).getState();
        assertEquals("state", query(service.callback("github", "the-code", state, "another-browser", null, null)).get("error"));
        // A refused state is used up as well.
        assertEquals("state", query(service.callback("github", "the-code", state, state, null, null)).get("error"));
        assertEquals("state", query(service.callback("github", "the-code", "made-up", "made-up", null, null)).get("error"));

        state = service.authorize("github", "/query", null).getState();
        Map<String, String> denied = query(service.callback("github", null, state, state, "access_denied", null));
        assertEquals("denied", denied.get("error"));
        assertEquals("/query", denied.get("redirect"));
        server.verify();
    }

    @Test
    public void aVerifiedEmailLinksTheExistingUserAndTheTicketIsUsedOnce()
    {
        providers.put(OAuthProvider.GITHUB, GITHUB);
        when(repository.findByEmail("octo@example.com")).thenReturn(Optional.of(user(7, "octo", "octo@example.com")));
        githubAnswers("[{\"email\":\"other@example.com\",\"primary\":false,\"verified\":true},{\"email\":\"Octo@Example.com\",\"primary\":true,\"verified\":true}]");

        Map<String, String> result = signIn("github", "/query?tab=1");
        server.verify();
        assertEquals("/query?tab=1", result.get("redirect"));
        ArgumentCaptor<UserOAuthEntity> link = ArgumentCaptor.forClass(UserOAuthEntity.class);
        verify(links).save(link.capture());
        assertEquals(7L, link.getValue().getUserId());
        assertEquals("github", link.getValue().getProvider());
        assertEquals("42", link.getValue().getSubject());
        verify(users, never()).saveOrUpdate(any(UserEntity.class));

        CommonResponse<Object> token = service.redeem(result.get("ticket"));
        assertTrue(token.getStatus());
        assertEquals("jwt:octo", token.getData());
        assertFalse(service.redeem(result.get("ticket")).getStatus());
    }

    @Test
    public void aLinkedAccountSignsInItsUser()
    {
        providers.put(OAuthProvider.GITHUB, GITHUB);
        when(links.findByProviderAndSubject("github", "42")).thenReturn(Optional.of(UserOAuthEntity.builder().id(3L).userId(9L).provider("github").subject("42").build()));
        when(repository.findById(9L)).thenReturn(Optional.of(user(9, "linked", null)));
        githubAnswers("[]");

        assertEquals("jwt:linked", service.redeem(signIn("github", null).get("ticket")).getData());
        verify(links, never()).save(any());

        UserEntity disabled = user(9, "linked", null);
        disabled.setActive(false);
        when(repository.findById(9L)).thenReturn(Optional.of(disabled));
        server.reset();
        githubAnswers("[]");
        assertEquals("inactive", signIn("github", null).get("error"));
    }

    @Test
    public void aNewAccountIsCreatedOnlyWhileRegistrationIsOpen()
    {
        providers.put(OAuthProvider.GITHUB, GITHUB);
        when(repository.findByUsername("octo_cat")).thenReturn(Optional.of(user(1, "octo_cat", null)));
        when(users.saveOrUpdate(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(11L);
            return CommonResponse.success(saved);
        });
        githubAnswers("[{\"email\":\"new@example.com\",\"primary\":true,\"verified\":true}]");

        assertEquals("jwt:", ((String) service.redeem(signIn("github", null).get("ticket")).getData()).substring(0, 4));
        ArgumentCaptor<UserEntity> created = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).saveOrUpdate(created.capture());
        assertTrue(created.getValue().getUsername().matches("octo_cat_\\d{4}"), created.getValue().getUsername());
        assertEquals("new@example.com", created.getValue().getEmail());
        verify(links).save(any());

        when(settings.auth()).thenReturn(AuthSettings.builder().registration(false).siteUrl(SITE).build());
        server.reset();
        githubAnswers("[]");
        assertEquals("registration", signIn("github", null).get("error"));
    }

    @Test
    public void openIdConnectUsesTheConfiguredEndpointsAndIgnoresUnverifiedEmails()
    {
        providers.put(OAuthProvider.OIDC, OIDC);
        when(settings.auth()).thenReturn(AuthSettings.builder().registration(false).siteUrl(SITE).build());
        when(repository.findByEmail("boss@example.com")).thenReturn(Optional.of(user(1, "admin", "boss@example.com")));
        server.expect(requestTo("https://sso.example.com/token"))
                .andRespond(withSuccess("{\"access_token\":\"sso-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://sso.example.com/userinfo"))
                .andExpect(header("Authorization", "Bearer sso-token"))
                .andRespond(withSuccess("{\"sub\":\"abc\",\"email\":\"boss@example.com\",\"email_verified\":false}", MediaType.APPLICATION_JSON));

        // The unverified email does not reach the administrator account, and sign-up is closed.
        assertEquals("registration", signIn("oidc", null).get("error"));
        server.verify();
        verify(repository, never()).findByEmail(anyString());
        verify(users, never()).issue(anyString(), anyLong());
    }

    @Test
    public void aFailingProviderEndsWithAnError()
    {
        providers.put(OAuthProvider.GITHUB, GITHUB);
        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withSuccess("{\"error\":\"bad_verification_code\"}", MediaType.APPLICATION_JSON));
        assertEquals("failed", signIn("github", null).get("error"));
        server.reset();
        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_GATEWAY));
        assertEquals("failed", signIn("github", null).get("error"));
        verify(users, never()).issue(anyString(), anyLong());
    }

    @Test
    public void clientSecretsAreKeptHiddenAndRequiredToTurnAProviderOn()
    {
        RuntimeConfigureService configures = mock(RuntimeConfigureService.class);
        when(configures.getEffective(anyString(), anyString(), any())).thenReturn(Map.of());
        when(configures.getEffective(eq("SYSTEM"), eq("OAUTH_GITHUB"), any())).thenReturn(GITHUB.toMap(OAuthProvider.GITHUB));
        when(configures.save(anyString(), anyString(), anyMap(), any())).thenReturn(CommonResponse.success(null));
        SystemSettingServiceImpl system = new SystemSettingServiceImpl(configures, mock(InitializerConfigure.class), mock(MailService.class));

        @SuppressWarnings("unchecked")
        Map<String, OAuthSettings> shown = (Map<String, OAuthSettings>) system.settings().get("oauth");
        assertEquals(List.of("github", "google", "oidc"), List.copyOf(shown.keySet()));
        assertEquals("", shown.get("github").getClientSecret());
        assertTrue(shown.get("github").isSecretSet());

        AuthSettings auth = AuthSettings.builder().build();
        // Turning on a provider without its client is refused, and nothing is saved.
        assertFalse(system.save(auth, new MailSettings(), Map.of("google", OAuthSettings.builder().enabled(true).clientId("g").build())).getStatus());
        assertFalse(system.save(auth, new MailSettings(), Map.of("oidc", OIDC.toBuilder().userinfoUrl("not a url").build())).getStatus());
        assertFalse(system.save(AuthSettings.builder().siteUrl("metaforge.example.com").build(), new MailSettings(), null).getStatus());
        verify(configures, never()).save(anyString(), anyString(), anyMap(), any());

        // An empty secret keeps the stored one, and the GitHub endpoints can not be changed.
        OAuthSettings edited = shown.get("github").toBuilder().clientId("gh-new").tokenUrl("https://attacker.example.com/token").build();
        assertTrue(system.save(auth, new MailSettings(), Map.of("github", edited)).getStatus());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> values = ArgumentCaptor.forClass(Map.class);
        verify(configures).save(eq("SYSTEM"), eq("OAUTH_GITHUB"), values.capture(), any());
        assertEquals(Map.of("enabled", "true", "clientId", "gh-new", "clientSecret", "gh-secret"), values.getValue());
    }
}
