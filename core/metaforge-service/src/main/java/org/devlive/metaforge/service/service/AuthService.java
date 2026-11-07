package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.AuthBody;
import org.devlive.metaforge.service.body.EmailCodeBody;
import org.devlive.metaforge.service.body.PasswordResetBody;

import java.util.Map;

/** Sign-in, sign-up and password reset, each feature only as the system settings allow. */
public interface AuthService
{
    /** What the sign-in and sign-up pages offer; public. */
    Map<String, Object> options();

    CommonResponse<Object> signin(AuthBody body);

    CommonResponse<?> signup(AuthBody body);

    /** Mails a code; answers with the seconds to wait before asking again. */
    CommonResponse<Long> sendCode(EmailCodeBody body);

    CommonResponse<Boolean> reset(PasswordResetBody body);
}
