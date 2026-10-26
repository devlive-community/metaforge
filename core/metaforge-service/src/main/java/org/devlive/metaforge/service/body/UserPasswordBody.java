package org.devlive.metaforge.service.body;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordBody
{
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
