package com.example.sales_expense_system.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;

import com.example.sales_expense_system.service.AuditLogService;
import com.example.sales_expense_system.service.JwtService;
import com.example.sales_expense_system.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService, AuditLogService auditLogService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;   
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        if (path.equals("/api/auth/login") || path.equals("/api/auth/verify-otp")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // No token → continue request
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
    // 1. Extract username FIRST (do not check expiration yet)
    username = jwtService.extractUsername(jwt);

    var user = userRepository.findByUsername(username).orElse(null);

    // 2. Basic validation
    if (user == null || user.getActiveToken() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Session invalid");
        return;
    }

    // 🚨 3. PRIORITY: SESSION CONFLICT (THIS FIXES YOUR PROBLEM)
    if (!jwt.equals(user.getActiveToken())) {

        auditLogService.log(
            "SESSION_CONFLICT",
            "AUTH",
            "auth",
            user.getUserId(),
            user.getUsername(),
            "User logged in on another device.",
            "Active token mismatch detected.",
            null,
            null,
            null,
            user,
            request
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"SESSION_CONFLICT\"}");
        return;
    }

    // Note: expiry is handled below by the ExpiredJwtException catch block.
    // extractUsername() above already throws ExpiredJwtException if the token
    // is expired, so no separate isTokenExpired() call is needed here.

} catch (ExpiredJwtException e) {

    String expiredUser = e.getClaims().getSubject();

    var user = userRepository.findByUsername(expiredUser).orElse(null);

    auditLogService.log(
        "SESSION_EXPIRED",
        "AUTH",
        "auth",
        null,
        expiredUser,
        "Session expired",
        "JWT token expired",
        null,
        null,
        null,
        user,
        request
    );

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("Session expired");
    return;

} catch (Exception e) {
    filterChain.doFilter(request, response);
    return;
}

        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}