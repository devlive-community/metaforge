package org.devlive.metaforge.service.settings;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Third-party sign-ins in progress. A sign-in started at the provider must come back within ten
 * minutes with its state; the ticket it ends with is exchanged for the token within a minute.
 * Both are used once.
 */
@Component
public class OAuthStates
{
    private final SecureRandom random = new SecureRandom();
    private final Cache<String, Pending> pending = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
    private final Cache<String, Object> tickets = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Getter
    @AllArgsConstructor
    public static final class Pending
    {
        private final String state;
        private final OAuthProvider provider;
        /** The PKCE code verifier sent with the code. */
        private final String verifier;
        /** The page to return to after signing in, already checked to be a path on this site. */
        private final String redirect;
    }

    public Pending begin(OAuthProvider provider, String redirect)
    {
        Pending value = new Pending(random(), provider, random(), redirect);
        pending.put(value.getState(), value);
        return value;
    }

    public Optional<Pending> take(String state)
    {
        if (state == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.asMap().remove(state));
    }

    /** Keeps the sign-in result until the page exchanges the returned ticket for it. */
    public String ticket(Object auth)
    {
        String ticket = random();
        tickets.put(ticket, auth);
        return ticket;
    }

    public Optional<Object> redeem(String ticket)
    {
        if (ticket == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tickets.asMap().remove(ticket));
    }

    /** The S256 code challenge of a verifier. */
    public static String challenge(String verifier)
    {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String random()
    {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
