package com.selfdriveconnect.vehicleservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Validates JWTs issued by user-service. This service does NOT issue
 * tokens — it only verifies the signature (using the same shared
 * secret as user-service) and reads the embedded claims.
 *
 * In production this shared-secret approach would typically be
 * replaced by asymmetric keys (user-service signs with a private key,
 * everyone else verifies with the matching public key) so the signing
 * secret never has to be distributed to every service. Shared-secret
 * HMAC is fine for a local/portfolio setup.
 */
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Returns the authenticated user if the token is valid and not
     * expired, or null if it's missing, malformed, or expired.
     */
    public AuthenticatedUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                return null;
            }

            String email = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            return new AuthenticatedUser(userId, email, roles);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
