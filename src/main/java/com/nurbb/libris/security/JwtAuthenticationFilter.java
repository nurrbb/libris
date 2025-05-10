package com.nurbb.libris.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("Gelen Authorization Header: [" + authHeader + "]");

        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            String token = authHeader.replaceFirst("(?i)^Bearer ", "").trim();
            System.out.println("Çıkarılan JWT Token: [" + token + "]");

            try {
                String email = jwtUtil.extractUsername(token);

                System.out.println(" E-posta: " + email);
                System.out.println(" SecurityContext boş mu? " + (SecurityContextHolder.getContext().getAuthentication() == null));

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtUtil.validateToken(token, userDetails)) {
                        System.out.println(" Token geçerli");

                        String role = jwtUtil.extractRole(token);
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role)); // ✅ ROLE_ eklendi

                        System.out.println("JWT'den gelen rol: " + role);
                        System.out.println("Yetkiler: " + authorities);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        System.out.println(" Token geçersiz");
                    }
                }

            } catch (Exception e) {
                System.out.println(" JWT işleme hatası: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
