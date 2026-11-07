package org.devlive.metaforge.service.settings;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Six-digit codes mailed for sign-up and password reset. A code lasts ten minutes, a new one is
 * sent at most once a minute per address, and five wrong tries discard it.
 */
@Component
public class EmailCodes
{
    public static final int MINUTES = 10;
    public static final long RESEND_SECONDS = 60;
    private static final int ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final Cache<String, Entry> codes = CacheBuilder.newBuilder()
            .expireAfterWrite(MINUTES, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    public enum Purpose
    {
        SIGNUP, RESET
    }

    @AllArgsConstructor
    private static final class Entry
    {
        private final String code;
        private final long sent;
        private int attempts;
    }

    /** Seconds before another code may be sent to the address; 0 when it may be sent now. */
    public long waiting(String email, Purpose purpose)
    {
        Entry entry = codes.getIfPresent(key(email, purpose));
        if (entry == null) {
            return 0;
        }
        long passed = (System.currentTimeMillis() - entry.sent) / 1000;
        return Math.max(0, RESEND_SECONDS - passed);
    }

    /** A new code for the address, replacing any earlier one. */
    public String issue(String email, Purpose purpose)
    {
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(key(email, purpose), new Entry(code, System.currentTimeMillis(), 0));
        return code;
    }

    public void discard(String email, Purpose purpose)
    {
        codes.invalidate(key(email, purpose));
    }

    /** Checks a code; a matching code is used up, and too many wrong ones discard it. */
    public synchronized boolean verify(String email, Purpose purpose, String code)
    {
        String key = key(email, purpose);
        Entry entry = codes.getIfPresent(key);
        if (entry == null || code == null) {
            return false;
        }
        if (entry.code.equals(code.trim())) {
            codes.invalidate(key);
            return true;
        }
        if (++entry.attempts >= ATTEMPTS) {
            codes.invalidate(key);
        }
        return false;
    }

    private static String key(String email, Purpose purpose)
    {
        return purpose.name() + ":" + email.trim().toLowerCase(Locale.ROOT);
    }
}
