package org.devlive.metaforge.server.auth;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.settings.FsSettings;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.service.body.AuthBody;
import org.devlive.metaforge.service.body.EmailCodeBody;
import org.devlive.metaforge.service.body.PasswordResetBody;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.service.UserService;
import org.devlive.metaforge.service.service.impl.AuthServiceImpl;
import org.devlive.metaforge.service.service.impl.SystemSettingServiceImpl;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.EmailCodes;
import org.devlive.metaforge.service.settings.MailService;
import org.devlive.metaforge.service.settings.MailSettings;
import org.devlive.metaforge.service.settings.OAuthSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.mail.MessagingException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthSettingsTest
{
    private static final MailSettings MAIL = MailSettings.builder().host("smtp.example.com").port(465).ssl(true).from("noreply@example.com").password("secret").build();

    private SystemSettingService settings;
    private MailService mail;
    private UserService users;
    private UserRepository repository;
    private EmailCodes codes;
    private AuthServiceImpl service;

    @BeforeEach
    public void setUp()
    {
        settings = mock(SystemSettingService.class);
        mail = mock(MailService.class);
        users = mock(UserService.class);
        repository = mock(UserRepository.class);
        codes = new EmailCodes();
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(settings.oauth(any())).thenReturn(new OAuthSettings());
        service = new AuthServiceImpl(settings, codes, mail, users, repository, encoder, mock(InitializerConfigure.class));
    }

    private void given(AuthSettings auth, MailSettings server)
    {
        when(settings.auth()).thenReturn(auth);
        when(settings.mail()).thenReturn(server);
    }

    private static AuthSettings.AuthSettingsBuilder auth()
    {
        return AuthSettings.builder().registration(true).rememberDays(30);
    }

    private static UserEntity user(String username, String email)
    {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    @Test
    public void emailFeaturesAreOfferedOnlyWhenTurnedOnAndMailCanBeSent()
    {
        given(auth().emailVerification(true).passwordReset(true).build(), new MailSettings());
        Map<String, Object> options = service.options();
        assertEquals(false, options.get("emailVerification"));
        assertEquals(false, options.get("passwordReset"));
        assertEquals(false, options.get("rememberMe"));

        given(auth().emailVerification(true).passwordReset(true).rememberMe(true).build(), MAIL);
        options = service.options();
        assertEquals(true, options.get("emailVerification"));
        assertEquals(true, options.get("passwordReset"));
        assertEquals(true, options.get("rememberMe"));
        assertFalse(service.sendCode(body("a@example.com", "unknown")).getStatus());
    }

    private static EmailCodeBody body(String email, String purpose)
    {
        EmailCodeBody body = new EmailCodeBody();
        body.setEmail(email);
        body.setPurpose(purpose);
        return body;
    }

    @Test
    public void signUpWithVerificationNeedsTheMailedCode()
            throws MessagingException
    {
        given(auth().emailVerification(true).build(), MAIL);
        when(users.saveOrUpdate(any(UserEntity.class))).thenReturn(CommonResponse.success(new UserEntity()));
        assertTrue(service.sendCode(body("New@Example.com", "signup")).getStatus());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(MAIL), eq("new@example.com"), anyString(), text.capture());
        String code = text.getValue().replaceAll("(?s).*?(\\d{6}).*", "$1");

        // Asking again within a minute is refused.
        assertFalse(service.sendCode(body("new@example.com", "signup")).getStatus());

        AuthBody signup = new AuthBody();
        signup.setUsername("new_user");
        signup.setPassword("secret1");
        signup.setEmail("new@example.com");
        signup.setCode("000000".equals(code) ? "111111" : "000000");
        assertFalse(service.signup(signup).getStatus());
        signup.setCode(code);
        assertTrue(service.signup(signup).getStatus());
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).saveOrUpdate(saved.capture());
        assertEquals("new@example.com", saved.getValue().getEmail());
        // A code is used once.
        assertFalse(service.signup(signup).getStatus());
    }

    @Test
    public void signUpChecksTheSwitchAndTheUsernameRule()
    {
        given(auth().registration(false).build(), MAIL);
        AuthBody signup = new AuthBody();
        signup.setUsername("valid_name");
        signup.setPassword("secret1");
        assertFalse(service.signup(signup).getStatus());

        given(auth().build(), MAIL);
        signup.setUsername("no spaces!");
        assertFalse(service.signup(signup).getStatus());
        verify(users, never()).saveOrUpdate(any(UserEntity.class));
    }

    @Test
    public void codesAreNotMailedToUnknownAddressesOrKeptWhenSendingFails()
            throws MessagingException
    {
        given(auth().passwordReset(true).build(), MAIL);
        assertTrue(service.sendCode(body("nobody@example.com", "reset")).getStatus());
        verify(mail, never()).send(any(), anyString(), anyString(), anyString());

        when(repository.findByEmail("known@example.com")).thenReturn(Optional.of(user("known", "known@example.com")));
        doThrow(new MessagingException("refused")).when(mail).send(any(), anyString(), anyString(), anyString());
        assertFalse(service.sendCode(body("known@example.com", "reset")).getStatus());
        assertEquals(0, codes.waiting("known@example.com", EmailCodes.Purpose.RESET));
    }

    @Test
    public void aMailedCodeResetsThePassword()
    {
        given(auth().passwordReset(true).build(), MAIL);
        UserEntity known = user("known", "known@example.com");
        when(repository.findByEmail("known@example.com")).thenReturn(Optional.of(known));
        String code = codes.issue("known@example.com", EmailCodes.Purpose.RESET);

        PasswordResetBody reset = new PasswordResetBody();
        reset.setEmail("known@example.com");
        reset.setCode(code);
        reset.setPassword("short");
        assertFalse(service.reset(reset).getStatus());
        reset.setPassword("new-secret");
        assertTrue(service.reset(reset).getStatus());
        assertEquals("hash:new-secret", known.getPassword());
        verify(repository).save(known);
    }

    @Test
    public void signInByEmailAndRememberedSignInsLastTheSetDays()
    {
        given(auth().emailSignin(true).rememberMe(true).rememberDays(7).build(), new MailSettings());
        when(repository.findByEmail("known@example.com")).thenReturn(Optional.of(user("known", "known@example.com")));
        when(users.authenticate(any(UserEntity.class), anyLong())).thenReturn(CommonResponse.success("token"));
        AuthBody signin = new AuthBody();
        signin.setUsername("Known@Example.com");
        signin.setPassword("secret1");
        signin.setRemember(true);
        service.signin(signin);
        ArgumentCaptor<UserEntity> user = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).authenticate(user.capture(), eq(TimeUnit.DAYS.toMillis(7)));
        assertEquals("known", user.getValue().getUsername());

        // Without the setting, the box is ignored and the default lifetime applies.
        given(auth().build(), new MailSettings());
        signin.setUsername("other");
        service.signin(signin);
        verify(users).authenticate(any(UserEntity.class), eq(0L));
    }

    @Test
    public void savedMailPasswordsAreKeptAndNeverSentBack()
    {
        RuntimeConfigureService configures = mock(RuntimeConfigureService.class);
        InitializerConfigure initializer = mock(InitializerConfigure.class);
        when(initializer.getRegistrationEnable()).thenReturn(true);
        when(initializer.getCaptchaEnable()).thenReturn(false);
        when(configures.getEffective(eq("SYSTEM"), eq("MAIL"), any())).thenReturn(MAIL.toMap());
        when(configures.getEffective(eq("SYSTEM"), eq("AUTH"), any())).thenReturn(Map.of("registration", "true"));
        when(configures.save(anyString(), anyString(), anyMap(), any())).thenReturn(CommonResponse.success(null));
        SystemSettingServiceImpl system = new SystemSettingServiceImpl(configures, initializer, mail, mock(PluginManager.class));

        MailSettings shown = (MailSettings) system.settings().get("mail");
        assertEquals("", shown.getPassword());
        assertTrue(shown.isPasswordSet());

        assertTrue(system.save(auth().build(), MAIL.toBuilder().password("").host("smtp.other.com").build(), null).getStatus());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> values = ArgumentCaptor.forClass(Map.class);
        verify(configures).save(eq("SYSTEM"), eq("MAIL"), values.capture(), any());
        assertEquals("secret", values.getValue().get("password"));
        assertEquals("smtp.other.com", values.getValue().get("host"));
    }

    @Test
    public void fileStorageKeepsItsSecretAndOnlyTakesInstalledCompletePlugins()
    {
        RuntimeConfigureService configures = mock(RuntimeConfigureService.class);
        InitializerConfigure initializer = mock(InitializerConfigure.class);
        PluginManager plugins = mock(PluginManager.class);
        when(plugins.getPluginInfos()).thenReturn(List.of(PluginMetadata.builder().name("LocalFs").type(PluginType.FILESYSTEM).build(),
                PluginMetadata.builder().name("AmazonS3Fs").type(PluginType.FILESYSTEM).build(),
                PluginMetadata.builder().name("MySQL").type(PluginType.CONNECTOR).build()));
        when(configures.getEffective(anyString(), anyString(), any())).thenReturn(Map.of());
        when(configures.getEffective(eq("SYSTEM"), eq("FS"), any())).thenReturn(Map.of("type", "AmazonS3Fs", "access", "key", "secret", "stored", "endpoint", "s3.example.com", "bucket", "files"));
        when(configures.save(anyString(), anyString(), anyMap(), any())).thenReturn(CommonResponse.success(null));
        SystemSettingServiceImpl system = new SystemSettingServiceImpl(configures, initializer, mail, plugins);

        FsSettings shown = (FsSettings) system.settings().get("fs");
        assertEquals("", shown.getSecret());
        assertTrue(shown.isSecretSet());
        assertEquals(List.of("AmazonS3Fs", "LocalFs"), system.settings().get("fsTypes"));

        FsSettings edited = system.fs().toBuilder().secret("").bucket("archive").build();
        assertTrue(system.save(auth().build(), new MailSettings(), null, edited).getStatus());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> values = ArgumentCaptor.forClass(Map.class);
        verify(configures).save(eq("SYSTEM"), eq("FS"), values.capture(), any());
        assertEquals("stored", values.getValue().get("secret"));
        assertEquals("archive", values.getValue().get("bucket"));

        assertFalse(system.save(auth().build(), new MailSettings(), null, FsSettings.builder().type("QiniuFs").build()).getStatus());
        assertFalse(system.save(auth().build(), new MailSettings(), null, edited.toBuilder().bucket("").build()).getStatus());
        assertTrue(system.save(auth().build(), new MailSettings(), null, FsSettings.builder().type("LocalFs").build()).getStatus());
    }

}
