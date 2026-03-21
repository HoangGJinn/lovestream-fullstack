package com.hcmute.lovestream.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Khóa bí mật (Lấy từ application.properties)
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshExpiration;

    // Lấy Username (Email) từ Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Lấy UserId từ Token (Custom claim)
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    // Lấy loại Token (ACCESS hoặc REFRESH)
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ----------------- GENERATE TOKENS -----------------

    // Dành cho luồng AuthServiceImpl đăng nhập đơn giản
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "ACCESS"); // Đảm bảo Access Token luôn có type để Filter phân biệt được với Refresh Token
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    // Dành cho Spring Security UserDetails (Access Token)
    public String generateAccessToken(UserDetails userDetails, Long userId) {
        return generateToken(new HashMap<>(), userDetails, userId, jwtExpiration, "ACCESS");
    }

    // Dành cho Spring Security UserDetails (Refresh Token)
    public String generateRefreshToken(UserDetails userDetails, Long userId) {
        return generateToken(new HashMap<>(), userDetails, userId, refreshExpiration, "REFRESH");
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, Long userId, Long expiration, String type) {
        extraClaims.put("userId", userId);
        extraClaims.put("type", type);
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    // ----------------- VALIDATE TOKENS -----------------

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean validateRefreshToken(String token) {
        try {
            return "REFRESH".equals(extractTokenType(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // Tiện ích gom chung việc kiểm tra cho các Controller (VD: CartController, OrderController)
    public boolean validateJwtToken(HttpServletRequest request, UserDetails userDetails) {
        String token = extractTokenFromRequest(request);
        if (token == null) return false;
        try {
            return validateToken(token, userDetails) && "ACCESS".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    // ----------------- EXTRACTION & UTILS -----------------

    public String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Ưu tiên lấy từ Cookie trước (Dành cho Thymeleaf / Giao diện Web)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Fallback sang lấy từ Header (Dành cho Postman / Mobile App)
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Cú pháp mới của jjwt 0.12.3
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Mã hóa SecretKey
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}