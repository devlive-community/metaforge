package org.devlive.metaforge.server.controller.user;

import com.fasterxml.jackson.annotation.JsonView;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.body.AuthBody;
import org.devlive.metaforge.service.body.EmailCodeBody;
import org.devlive.metaforge.service.body.PasswordResetBody;
import org.devlive.metaforge.service.service.AuthService;
import org.devlive.metaforge.service.validation.ValidationGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    private final AuthService authService;

    public AuthController(AuthService authService)
    {
        this.authService = authService;
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
}
