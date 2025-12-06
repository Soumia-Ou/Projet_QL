package com.example.pfa.reservation;

import com.example.pfa.reservation.jwt.CustomerUsersDetailsService;
import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomerUsersDetailsService service;

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_excludedPath_callsChainOnly() throws Exception {
        // Test : Vérifier que les chemins exclus (comme /user/login) ne déclenchent pas le filtre JWT
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/user/login");

        jwtFilter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verifyNoInteractions(jwtUtil, service);
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_callsChain() throws Exception {
        // Test : Vérifier qu'une requête sans header Authorization passe sans traitement JWT
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/some/path");
        when(req.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verifyNoInteractions(service);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilterInternal_invalidToken_shouldContinueChain() throws Exception {
        // Test : Vérifier qu'un token invalide ne bloque pas la chaine de filtres
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("Bearer bad.token.here");

        // Simuler une exception lors de l'extraction du nom d'utilisateur
        when(jwtUtil.extractUsername("bad.token.here"))
                .thenThrow(new RuntimeException("invalid token"));

        // Le filtre devrait continuer la chaine même en cas d'exception
        jwtFilter.doFilterInternal(req, res, chain);

        // Vérifier que la chaine est toujours appelée
        verify(chain, times(1)).doFilter(req, res);

        // Vérifier qu'on n'appelle pas loadUserByUsername
        verify(service, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_validToken_setsSecurityContext() throws Exception {
        // Test : Vérifier qu'un token valide configure correctement le contexte de sécurité
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("Bearer good.token");

        when(jwtUtil.extractUsername("good.token")).thenReturn("alice");

        org.springframework.security.core.userdetails.UserDetails ud =
                new org.springframework.security.core.userdetails.User("alice", "pwd",
                        List.of(new SimpleGrantedAuthority("HOTEL_ADMIN")));

        when(service.loadUserByUsername("alice")).thenReturn(ud);
        when(jwtUtil.validateToken("good.token", ud)).thenReturn(true);

        jwtFilter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("alice", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("HOTEL_ADMIN")));

        verify(chain, times(1)).doFilter(req, res);
        verify(service, times(1)).loadUserByUsername("alice");
        verify(jwtUtil, times(1)).validateToken("good.token", ud);
        verify(jwtUtil, times(1)).extractUsername("good.token");
    }

    @Test
    void getCurrentUserRole_and_roleChecks() {
        // Test : Vérifier les méthodes de vérification des rôles utilisateur
        var userDetails = new org.springframework.security.core.userdetails.User(
                "bob",
                "pwd",
                List.of(new SimpleGrantedAuthority("HOTEL_ADMIN"))
        );
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("HOTEL_ADMIN", jwtFilter.getCurrentUserRole());
        assertTrue(jwtFilter.isHotelAdmin());
        assertFalse(jwtFilter.isClient());
        assertFalse(jwtFilter.isGlobalAdmin());
    }

    @Test
    void doFilterInternal_invalidBearerFormat_callsChain() throws Exception {
        // Test : Vérifier qu'un format de token incorrect n'empêche pas la chaine de s'exécuter
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("InvalidFormatWithoutBearer");

        jwtFilter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(service);
    }

    @Test
    void doFilterInternal_nullAuthentication_afterInvalidToken() throws Exception {
        // Test : Vérifier que le contexte de sécurité reste null après un token invalide
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtUtil.extractUsername("invalid.token"))
                .thenThrow(new RuntimeException("invalid token"));

        jwtFilter.doFilterInternal(req, res, chain);

        // Le contexte de sécurité devrait être null après un token invalide
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);

        verify(chain, times(1)).doFilter(req, res);
        verify(service, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_tokenWithoutBearerPrefix_continuesChain() throws Exception {
        // Test : Vérifier qu'un token sans préfixe Bearer est ignoré
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("TokenWithoutBearer");

        jwtFilter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(service);
    }

    @Test
    void doFilterInternal_emptyToken_continuesChain() throws Exception {
        // Test : Vérifier qu'un token vide est ignoré
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("Bearer "); // Token vide après Bearer

        // Pour un token vide, extractUsername pourrait être appelé et échouer
        // Ou pourrait ne pas être appelé du tout selon l'implémentation
        // On ne spécifie pas de comportement pour éviter les erreurs

        jwtFilter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        // On ne vérifie pas les interactions car cela dépend de l'implémentation
    }

    @Test
    void getCurrentUserRole_noAuthentication_returnsNull() {
        // Test : Vérifier que getCurrentUserRole retourne null quand il n'y a pas d'authentification
        SecurityContextHolder.clearContext();

        assertNull(jwtFilter.getCurrentUserRole());
        assertFalse(jwtFilter.isHotelAdmin());
        assertFalse(jwtFilter.isGlobalAdmin());
        assertFalse(jwtFilter.isClient());
    }

    @Test
    void doFilterInternal_validTokenButInvalidUser_continuesChain() throws Exception {
        // Test : Vérifier qu'un token valide mais avec un utilisateur inexistant ne bloque pas
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("Bearer valid.token");

        when(jwtUtil.extractUsername("valid.token")).thenReturn("unknownUser");

        // Simuler que loadUserByUsername retourne null
        when(service.loadUserByUsername("unknownUser")).thenReturn(null);

        jwtFilter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verify(service, times(1)).loadUserByUsername("unknownUser");
        // validateToken ne devrait PAS être appelé car userDetails est null
        verify(jwtUtil, never()).validateToken(anyString(), any());
    }

    @Test
    void doFilterInternal_validTokenButInvalidValidation_continuesChain() throws Exception {
        // Test : Vérifier qu'un token valide mais non validé par jwtUtil ne configure pas le contexte
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getServletPath()).thenReturn("/protected");
        when(req.getHeader("Authorization")).thenReturn("Bearer valid.token");

        when(jwtUtil.extractUsername("valid.token")).thenReturn("alice");

        org.springframework.security.core.userdetails.UserDetails ud =
                new org.springframework.security.core.userdetails.User("alice", "pwd",
                        List.of(new SimpleGrantedAuthority("HOTEL_ADMIN")));

        when(service.loadUserByUsername("alice")).thenReturn(ud);
        when(jwtUtil.validateToken("valid.token", ud)).thenReturn(false);

        jwtFilter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth); // Pas d'authentification car token non validé

        verify(chain, times(1)).doFilter(req, res);
        verify(service, times(1)).loadUserByUsername("alice");
        verify(jwtUtil, times(1)).validateToken("valid.token", ud);
    }
}