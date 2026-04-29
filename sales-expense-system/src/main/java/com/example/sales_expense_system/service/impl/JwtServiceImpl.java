package com.example.sales_expense_system.service.impl;

import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.service.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final String SECRET = "sales-expense-system-super-secret-key-123456789";

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    private final long expiration = 1000 * 60 * 5; // 1hr

    @Override
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole().getRoleName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateToken(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername()) && !isTokenExpired(token));
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {

        final String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    @Override
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    private boolean isTokenExpired(String token) {

        Date expirationDate = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return expirationDate.before(new Date());
    }
}