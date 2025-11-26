package com.example.pfa.reservation.jwt;

import com.example.pfa.reservation.model.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomerUsersDetailsService service;

    public JwtFilter(JwtUtil jwtUtil, CustomerUsersDetailsService service) {
        this.jwtUtil = jwtUtil;
        this.service = service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getServletPath().matches("^(/user/login|/user/forgotPassword|/user/signup)$")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        String userName = null;
        String token = null;
        Claims claims = null;
        String role = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);

            // Extraire username et claims UNIQUEMENT après avoir obtenu le token
            userName = jwtUtil.extractUsername(token);
            claims = jwtUtil.extractAllClaims(token);

            // Récupérer le rôle depuis le token
            role = claims.get("role", String.class);
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = service.loadUserByUsername(userName);
            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    public boolean isHotelAdmin() {
        String role = getCurrentUserRole();
        return Role.HOTEL_ADMIN.name().equalsIgnoreCase(role);
    }

    public boolean isGlobalAdmin() {
        String role = getCurrentUserRole();
        return Role.GLOBAL_ADMIN.name().equalsIgnoreCase(role);
    }

    public boolean isClient() {
        String role = getCurrentUserRole();
        return Role.CLIENT.name().equalsIgnoreCase(role);
    }

    // Méthode utilitaire pour extraire le rôle depuis le JWT
    public String getCurrentUserRole() {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails userDetails) {

            // Si tu stockes le rôle dans les authorities
            return userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse(null);
        }
        return null;
    }


}
