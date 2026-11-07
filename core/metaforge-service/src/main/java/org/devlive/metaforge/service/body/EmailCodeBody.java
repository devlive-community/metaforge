package org.devlive.metaforge.service.body;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Asks for a code mailed to an address, for sign-up (SIGNUP) or a password reset (RESET). */
@Data
@NoArgsConstructor
public class EmailCodeBody
{
    private String email;
    private String purpose;
}
