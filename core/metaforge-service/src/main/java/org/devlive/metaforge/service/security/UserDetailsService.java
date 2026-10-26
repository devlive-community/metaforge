package org.devlive.metaforge.service.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.compress.utils.Sets;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UserDetailsService
        implements UserDetails
{
    @Getter
    @Setter
    @JsonIgnore
    private Long id;
    @JsonIgnore
    private String password;
    private String code;
    private String username;
    private Collection<? extends GrantedAuthority> authorities;
    private String avatar;

    public UserDetailsService(Long id, String code, String username, String password,
            Collection<? extends GrantedAuthority> authorities,
            String avatar)
    {
        this.id = id;
        this.code = code;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.avatar = avatar;
    }

    public static UserDetailsService build(UserEntity user)
    {
        // TODO: Add no code alert
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .collect(Collectors.toList());
        String avatar = null;
        if (user.getAvatarConfigure() != null) {
            avatar = user.getAvatarConfigure().getPath();
        }
        return new UserDetailsService(
                user.getId(),
                user.getCode(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                avatar
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return authorities;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public String getUsername()
    {
        return username;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return true;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    public String getCode()
    {
        return code;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public static UserEntity getUser()
    {
        UserEntity userInfo = new UserEntity();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication)) {
            Object principal = authentication.getPrincipal();
            if (Objects.nonNull(principal)) {
                UserDetailsService loginPrincipalUserInfo = (UserDetailsService) principal;
                userInfo.setUsername(loginPrincipalUserInfo.getUsername());
                userInfo.setCode(loginPrincipalUserInfo.getCode());
                userInfo.setId(loginPrincipalUserInfo.getId());

                Set<RoleEntity> roles = Sets.newHashSet();
                loginPrincipalUserInfo.getAuthorities()
                        .forEach(v -> {
                            roles.add(RoleEntity.builder()
                                    .code(v.getAuthority())
                                    .build());
                        });
                userInfo.setRoles(roles);
            }
        }
        return userInfo;
    }
}
