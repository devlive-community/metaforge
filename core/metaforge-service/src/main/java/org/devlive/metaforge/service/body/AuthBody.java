package org.devlive.metaforge.service.body;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.service.validation.ValidationGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AuthBody
{
    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class,
            ValidationGroup.Crud.Auth.class
    })
    @Size(max = 20)
    private String username;

    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class,
            ValidationGroup.Crud.Auth.class
    })
    @Size(max = 120)
    private String password;
    private Integer captcha;
    private Long timestamp;
}
