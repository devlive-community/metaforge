package org.devlive.metaforge.service.security;

import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Runs an action as another user, so ownership checks apply as for that user, then restores the
 * caller's identity. Used where an owner shares something whose reads are checked against them.
 */
public final class RunAs
{
    private RunAs()
    {}

    /** Without a user the action runs anonymously, where ownership checks deny access. */
    public static <T> T user(UserEntity user, Supplier<T> action)
    {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        if (user != null) {
            UserDetailsService principal = new UserDetailsService(user.getId(), user.getCode(), user.getUsername(), "", new ArrayList<>(), null);
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        }
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        }
        finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
