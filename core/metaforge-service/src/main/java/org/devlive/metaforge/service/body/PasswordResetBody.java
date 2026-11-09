package org.devlive.metaforge.service.body;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Sets a new password for the account of an email whose code was mailed. */
@Data
@NoArgsConstructor
public class PasswordResetBody
{
    private String email;
    private String code;
    private String password;
}
