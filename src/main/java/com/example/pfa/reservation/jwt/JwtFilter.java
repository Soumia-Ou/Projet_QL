package com.example.pfa.reservation.jwt;

import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.UserDAO;
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
    private final UserDAO userDao;

    public JwtFilter(JwtUtil jwtUtil, CustomerUsersDetailsService service, UserDAO userDao) {
        this.jwtUtil = jwtUtil;
        this.service = service;
        this.userDao = userDao;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {

        if (request.getServletPath().matches("^(/user/login|/user/forgotPassword|/user/signup)$")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        String userName = null;
        String token = null;

        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7).trim(); // Ajout de trim() pour enlever les espaces

                // Vérifier que le token n'est pas vide après trim()
                if (!token.isEmpty()) {
                    userName = jwtUtil.extractUsername(token);
                }
            }
        } catch (Exception e) {
            // Token invalide ou expiré - continuer sans authentification
            logger.warn("JWT token validation failed", e);
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = service.loadUserByUsername(userName);

            // IMPORTANT: Vérifier que userDetails n'est pas null avant d'appeler validateToken
            if (userDetails != null && jwtUtil.validateToken(token, userDetails)) {
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

    public String getCurrentUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }


    public Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username != null) {

            User user = userDao.findByEmailOrUsername(username, username);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}