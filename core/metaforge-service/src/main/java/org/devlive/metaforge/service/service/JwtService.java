package org.devlive.metaforge.service.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.Date;

@Slf4j
@Component
public class JwtService
{
    private static final String DEFAULT_SECRET = "MetaForgeSecretKey";

    @Value("${metaforge.security.secret}")
    private String jwtSecret = DEFAULT_SECRET;

    @Value("${metaforge.security.expiration}")
    private int jwtExpirationMs = 86400000;

    @PostConstruct
    public void warnOnDefaultSecret()
    {
        // 默认密钥是公开已知的，攻击者可以直接伪造任意用户的合法令牌
        if (DEFAULT_SECRET.equals(jwtSecret)) {
            log.warn("==================================================================================");
            log.warn("MetaForge is running with the well-known default JWT secret (MetaForgeSecretKey).");
            log.warn("Anyone can forge a valid token for any user, including admin. Configure a unique");
            log.warn("random value for 'metaforge.security.secret' before exposing this server.");
            log.warn("==================================================================================");
        }
    }

    public String generateJwtToken(Authentication authentication)
    {
        UserDetailsService userPrincipal = (UserDetailsService) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String getUserNameFromJwtToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken)
    {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        }
        catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        }
        catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        }
        catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        }
        catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
