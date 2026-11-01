package org.devlive.metaforge.server.controller.user;

import com.fasterxml.jackson.annotation.JsonView;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.body.AuthBody;
import org.devlive.metaforge.service.body.EmailCodeBody;
import org.devlive.metaforge.service.body.PasswordResetBody;
import org.devlive.metaforge.service.service.AuthService;
import org.devlive.metaforge.service.service.OAuthService;
import org.devlive.metaforge.service.validation.ValidationGroup;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    /** Keeps the state of a third-party sign-in in the browser that started it. */
    private static final String STATE_COOKIE = "metaforge_oauth_state";

    private final AuthService authService;
    private final OAuthService oauthService;

    public AuthController(AuthService authService, OAuthService oauthService)
    {
        this.authService = authService;
        this.oauthService = oauthService;
    }

    /** What the sign-in and sign-up pages offer, as set in system settings. */
    @GetMapping("/options")
    public CommonResponse<Map<String, Object>> options()
    {
        return CommonResponse.success(authService.options());
    }

    @PostMapping("/signin")
    public CommonResponse<Object> authenticateUser(@RequestBody @Validated(ValidationGroup.Crud.Auth.class) AuthBody configure)
    {
        return authService.signin(configure);
    }

    @PostMapping("/signup")
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<?> registerUser(@RequestBody @Validated(ValidationGroup.Crud.Create.class) AuthBody configure)
    {
        return authService.signup(configure);
    }

    @PostMapping("/code")
    public CommonResponse<Long> sendCode(@RequestBody EmailCodeBody configure)
    {
        return authService.sendCode(configure);
    }

    @PostMapping("/reset")
    public CommonResponse<Boolean> resetPassword(@RequestBody PasswordResetBody configure)
    {
        return authService.reset(configure);
    }

    /** Sends the browser to the provider's sign-in page, or back to the sign-in page when the provider is off. */
    @GetMapping("/oauth/{provider}/authorize")
    public void authorize(@PathVariable String provider, @RequestParam(required = false) String redirect, HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        OAuthService.Redirect target = oauthService.authorize(provider, redirect, base());
        if (target.getState() != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, stateCookie(request, target.getState(), Duration.ofMinutes(10)));
        }
        response.sendRedirect(target.getLocation());
    }

    /** Where the provider sends the browser back; it goes on to the sign-in callback page. */
    @GetMapping("/oauth/{provider}/callback")
    public void callback(@PathVariable String provider, @RequestParam(required = false) String code, @RequestParam(required = false) String state,
            @RequestParam(required = false) String error, @CookieValue(name = STATE_COOKIE, required = false) String browserState,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie(request, "", Duration.ZERO));
        response.sendRedirect(oauthService.callback(provider, code, state, browserState, error, base()));
    }

    /** The token of a finished third-party sign-in, for the ticket the callback page was given. */
    @PostMapping("/oauth/ticket")
    public CommonResponse<Object> ticket(@RequestBody Map<String, String> body)
    {
        return oauthService.redeem(body == null ? null : body.get("ticket"));
    }

    private static String base()
    {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    private static String stateCookie(HttpServletRequest request, String value, Duration age)
    {
        // Lax, so it comes along when the provider sends the browser back.
        return ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path(request.getContextPath() + "/api/auth/oauth")
                .maxAge(age)
                .build()
                .toString();
    }
}
