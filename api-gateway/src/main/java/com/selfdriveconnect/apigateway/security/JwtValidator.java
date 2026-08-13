package com.selfdriveconnect.apigateway.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Validates JWTs issued by user-service, using the same shared secret
 * every other service validates against. The gateway's check is
 * intentionally shallow — signature valid + not expired — it does NOT
 * decode roles or make authorization decisions. That's still each
 * service's job. This is a pre-filter to reject obviously-bad requests
 * early (missing/garbage/expired tokens), not a replacement for the
 * real per-service ownership/role checks. Never trust a gateway alone.
 */
@Component
public class JwtValidator {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
