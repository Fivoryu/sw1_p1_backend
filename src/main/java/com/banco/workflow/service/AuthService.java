package com.banco.workflow.service;

import java.security.Key;
import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.banco.workflow.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para manejar JWT tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${spring.security.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.security.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Genera un JWT token para un usuario
     */
    public String generateToken(User user) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

            return Jwts.builder()
                    .setSubject(user.getId())
                    .claim("username", user.getUsername())
                    .claim("email", user.getEmail())
                    .claim("roles", user.getRoles())
                    .claim("departamento", user.getDepartamento())
                    .claim("empresa", user.getEmpresa())
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            log.error("Error al generar JWT token", e);
            throw new RuntimeException("Error al generar token", e);
        }
    }

    /**
     * Obtiene el ID del usuario desde un JWT token
     */
    public String getUserIdFromToken(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            log.error("Error al parsear JWT token", e);
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            return parseClaims(token).get("username", String.class);
        } catch (Exception e) {
            log.error("Error al obtener username del token", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        try {
            Object roles = parseClaims(token).get("roles");
            if (roles instanceof java.util.Collection<?> collection) {
                return collection.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("Error al obtener roles del token", e);
            return Set.of();
        }
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public String getEmpresaFromToken(String token) {
        try {
            return parseClaims(token).get("empresa", String.class);
        } catch (Exception e) {
            log.error("Error al obtener empresa del token", e);
            return null;
        }
    }

    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
