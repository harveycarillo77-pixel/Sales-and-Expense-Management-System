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
            username = jwtService.extractUsername(jwt);

        } catch (ExpiredJwtException e) {
        
            String expiredUser = e.getClaims().getSubject();

            var user = userRepository.findByUsername(expiredUser).orElse(null);
        
             auditLogService.log(
                "SESSION_EXPIRED",     // action
                "AUTH",                // moduleName
                "auth",                // tableName
                null,                  // recordId
                expiredUser,           // targetDisplay
                "Session expired",     // summary
                "JWT token expired",   // message
                null,
                null,
                null,
                user,
                request
            );
        
            // Optional: send 401 so frontend knows
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Session expired");
            return;
        
        } catch (Exception e) {
            // Invalid token (tampered, malformed, etc.)
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