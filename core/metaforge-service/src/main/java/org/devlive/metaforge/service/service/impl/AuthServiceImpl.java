package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.captcha.entity.ResultEntity;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.AuthBody;
import org.devlive.metaforge.service.body.EmailCodeBody;
import org.devlive.metaforge.service.body.PasswordResetBody;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.service.AuthService;
import org.devlive.metaforge.service.service.SystemSettingService;
import org.devlive.metaforge.service.service.UserService;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.EmailCodes;
import org.devlive.metaforge.service.settings.MailService;
import org.devlive.metaforge.service.settings.MailSettings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class AuthServiceImpl
        implements AuthService
{
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 120;

    private final SystemSettingService settings;
    private final EmailCodes codes;
    private final MailService mailService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final InitializerConfigure initializer;

    public AuthServiceImpl(SystemSettingService settings, EmailCodes codes, MailService mailService, UserService userService, UserRepository userRepository,
            PasswordEncoder encoder, InitializerConfigure initializer)
    {
        this.settings = settings;
        this.codes = codes;
        this.mailService = mailService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.initializer = initializer;
    }

    static boolean isEmail(String value)
    {
        return value != null && value.length() <= 255 && EMAIL.matcher(value.trim()).matches();
    }

    private static String normalize(String email)
    {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Email verification and password reset only count once mail can be sent. */
    private static boolean verification(AuthSettings auth, MailSettings mail)
    {
        return auth.isRegistration() && auth.isEmailVerification() && mail.ready();
    }

    private static boolean reset(AuthSettings auth, MailSettings mail)
    {
        return auth.isPasswordReset() && mail.ready();
    }

    @Override
    public Map<String, Object> options()
    {
        AuthSettings auth = settings.auth();
        MailSettings mail = settings.mail();
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("registration", auth.isRegistration());
        options.put("captcha", auth.isCaptcha());
        options.put("rememberMe", auth.isRememberMe());
        options.put("rememberDays", auth.getRememberDays());
        options.put("emailSignin", auth.isEmailSignin());
        options.put("emailVerification", verification(auth, mail));
        options.put("passwordReset", reset(auth, mail));
        options.put("termsUrl", auth.getTermsUrl());
        options.put("privacyUrl", auth.getPrivacyUrl());
        // Third-party sign-in is listed here once a provider can be configured.
        options.put("providers", Collections.emptyList());
        return options;
    }

    private CommonResponse<Object> captcha(AuthSettings auth, AuthBody body)
    {
        if (!auth.isCaptcha()) {
            return null;
        }
        if (ObjectUtils.isEmpty(body.getCaptcha()) || body.getTimestamp() == null) {
            return CommonResponse.failure("Captcha must not be empty");
        }
        ResultEntity entity = initializer.getCache().getIfPresent(body.getTimestamp());
        if (entity == null) {
            return CommonResponse.failure("Captcha does not exists");
        }
        if (!entity.getResult().equals(body.getCaptcha())) {
            initializer.getCache().invalidate(body.getTimestamp());
            return CommonResponse.failure("The calculation result is incorrect");
        }
        return null;
    }

    @Override
    public CommonResponse<Object> signin(AuthBody body)
    {
        AuthSettings auth = settings.auth();
        CommonResponse<Object> failed = captcha(auth, body);
        if (failed != null) {
            return failed;
        }
        String username = body.getUsername();
        if (auth.isEmailSignin() && isEmail(username)) {
            Optional<UserEntity> owner = userRepository.findByEmail(normalize(username));
            if (owner.isPresent()) {
                username = owner.get().getUsername();
            }
        }
        long days = auth.isRememberMe() && Boolean.TRUE.equals(body.getRemember()) ? auth.getRememberDays() : 0;
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(body.getPassword());
        return userService.authenticate(user, TimeUnit.DAYS.toMillis(days));
    }

    @Override
    public CommonResponse<?> signup(AuthBody body)
    {
        AuthSettings auth = settings.auth();
        if (!auth.isRegistration()) {
            return CommonResponse.failure("Registration is disabled");
        }
        CommonResponse<Object> failed = captcha(auth, body);
        if (failed != null) {
            return failed;
        }
        if (body.getUsername() == null || !USERNAME.matcher(body.getUsername()).matches()) {
            return CommonResponse.failure("The username has 3 to 32 letters, digits or underscores");
        }
        UserEntity user = new UserEntity();
        if (verification(auth, settings.mail())) {
            if (!isEmail(body.getEmail())) {
                return CommonResponse.failure("Enter a valid email address");
            }
            String email = normalize(body.getEmail());
            if (userRepository.findByEmail(email).isPresent()) {
                return CommonResponse.failure("The email is already registered");
            }
            if (!codes.verify(email, EmailCodes.Purpose.SIGNUP, body.getCode())) {
                return CommonResponse.failure("The email code is wrong or has expired");
            }
            user.setEmail(email);
        }
        user.setUsername(body.getUsername());
        user.setPassword(body.getPassword());
        return userService.saveOrUpdate(user);
    }

    @Override
    public CommonResponse<Long> sendCode(EmailCodeBody body)
    {
        AuthSettings auth = settings.auth();
        MailSettings mail = settings.mail();
        EmailCodes.Purpose purpose;
        try {
            purpose = EmailCodes.Purpose.valueOf(StringUtils.defaultString(body.getPurpose()).toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            return CommonResponse.failure("Unknown code purpose");
        }
        boolean enabled = purpose == EmailCodes.Purpose.SIGNUP ? verification(auth, mail) : reset(auth, mail);
        if (!enabled) {
            return CommonResponse.failure("This feature is not enabled");
        }
        if (!isEmail(body.getEmail())) {
            return CommonResponse.failure("Enter a valid email address");
        }
        String email = normalize(body.getEmail());
        long waiting = codes.waiting(email, purpose);
        if (waiting > 0) {
            return CommonResponse.failure("Wait " + waiting + " seconds before asking for another code");
        }
        boolean registered = userRepository.findByEmail(email).isPresent();
        if (purpose == EmailCodes.Purpose.SIGNUP && registered) {
            return CommonResponse.failure("The email is already registered");
        }
        String code = codes.issue(email, purpose);
        // A reset for an unknown address answers the same way, so addresses can not be probed.
        if (purpose == EmailCodes.Purpose.RESET && !registered) {
            return CommonResponse.success(EmailCodes.RESEND_SECONDS);
        }
        try {
            mailService.send(mail, email, "MetaForge 验证码 / Verification code", String.format(
                    "你的 MetaForge 验证码是 %s，%d 分钟内有效。如非本人操作请忽略本邮件。%nYour MetaForge verification code is %s. It expires in %d minutes; ignore this mail if you did not ask for it.",
                    code, EmailCodes.MINUTES, code, EmailCodes.MINUTES));
        }
        catch (MessagingException | RuntimeException e) {
            codes.discard(email, purpose);
            log.warn("Sending a code to {} failed", email, e);
            return CommonResponse.failure("Failed to send mail: " + e.getMessage());
        }
        return CommonResponse.success(EmailCodes.RESEND_SECONDS);
    }

    @Override
    public CommonResponse<Boolean> reset(PasswordResetBody body)
    {
        if (!reset(settings.auth(), settings.mail())) {
            return CommonResponse.failure("This feature is not enabled");
        }
        if (!isEmail(body.getEmail())) {
            return CommonResponse.failure("Enter a valid email address");
        }
        String password = body.getPassword();
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return CommonResponse.failure("The password has 6 to 120 characters");
        }
        String email = normalize(body.getEmail());
        if (!codes.verify(email, EmailCodes.Purpose.RESET, body.getCode())) {
            return CommonResponse.failure("The email code is wrong or has expired");
        }
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return CommonResponse.failure("The email code is wrong or has expired");
        }
        UserEntity value = user.get();
        value.setPassword(encoder.encode(password));
        userRepository.save(value);
        return CommonResponse.success(Boolean.TRUE);
    }
}
