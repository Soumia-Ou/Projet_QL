package com.example.pfa.reservation.JWT;

import com.example.pfa.reservation.model.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomerUsersDetailsService service;

    Claims claims =null;
    private  String userName =null;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getServletPath().matches("^(/user/login|/user/forgotPassword|/user/signup)$")) {
            filterChain.doFilter(request, response);
        }
        else{
            String authorizationHeader = request.getHeader("Authorization");
            String token = null;

            if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
                token = authorizationHeader.substring(7);
                userName = jwtUtil.extractUsername(token);
                claims = jwtUtil.extractAllClaims(token);
            }

            if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = service.loadUserByUsername(userName);
                if(jwtUtil.validateToken(token, userDetails)){
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            }
            filterChain.doFilter(request, response);
        }
    }

    public boolean isGlobalAdmin() {
        return claims != null && Role.GLOBAL_ADMIN.name().equalsIgnoreCase((String) claims.get("role"));
    }

    public boolean isHotelAdmin() {
        return claims != null && Role.HOTEL_ADMIN.name().equalsIgnoreCase((String) claims.get("role"));
    }

    public boolean isClient() {
        return claims != null && Role.CLIENT.name().equalsIgnoreCase((String) claims.get("role"));
    }


    public String getCurreentUser(){
        return userName;
    }
}
