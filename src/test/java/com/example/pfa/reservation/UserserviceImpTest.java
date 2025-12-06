package com.example.pfa.reservation;

import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.jwt.CustomerUsersDetailsService;
import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.jwt.JwtUtil;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.service.impl.UserserviceImp;
import com.example.pfa.reservation.utils.EmailUtils;
import com.example.pfa.reservation.wrapper.UserWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserserviceImpTest {

    @Mock
    private UserDAO userDao;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomerUsersDetailsService customerUsersDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtFilter jwtFilter;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailUtils emailUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserserviceImp userService;

    private User testUser;
    private Map<String, String> validRequestMap;

    @BeforeEach
    void setUp() {
        // Configuration d'un utilisateur de test pour tous les tests
        testUser = new User();
        testUser.setId(1L);
        testUser.setNom("Doe");
        testUser.setPrenom("John");
        testUser.setEmail("john.doe@example.com");
        testUser.setUserName("johndoe");
        testUser.setPassword("encodedPassword");
        testUser.setTelephone("0123456789");
        testUser.setRole(Role.CLIENT);

        // Configuration d'une requête d'inscription valide pour les tests de signUp
        validRequestMap = new HashMap<>();
        validRequestMap.put("nom", "Doe");
        validRequestMap.put("prenom", "John");
        validRequestMap.put("email", "john.doe@example.com");
        validRequestMap.put("userName", "johndoe");
        validRequestMap.put("password", "password123");
        validRequestMap.put("telephone", "0123456789");
        validRequestMap.put("role", "CLIENT");
    }

    // ==================== TESTS POUR LA MÉTHODE signUp() ====================

    @Test
    void signUp_ShouldReturnSuccess_WhenValidData() {
        // Test : Vérifier qu'une inscription réussit avec des données valides
        when(userDao.findByEmailOrUsername(anyString(), anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userDao.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<String> response = userService.signUp(validRequestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully Registered"));
        verify(userDao, times(1)).save(any(User.class));
    }

    @Test
    void signUp_ShouldReturnBadRequest_WhenEmailAlreadyExists() {
        // Test : Vérifier qu'une inscription échoue si l'email existe déjà
        User existingUser = new User();
        existingUser.setEmail(validRequestMap.get("email"));
        existingUser.setUserName("differentUsername");
        when(userDao.findByEmailOrUsername(anyString(), anyString())).thenReturn(existingUser);

        ResponseEntity<String> response = userService.signUp(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Email already exists"));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void signUp_ShouldReturnBadRequest_WhenUsernameAlreadyExists() {
        // Test : Vérifier qu'une inscription échoue si le nom d'utilisateur existe déjà
        User existingUser = new User();
        existingUser.setUserName(validRequestMap.get("userName"));
        existingUser.setEmail("different@email.com");
        when(userDao.findByEmailOrUsername(anyString(), anyString())).thenReturn(existingUser);

        ResponseEntity<String> response = userService.signUp(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Username already exists"));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void signUp_ShouldReturnInternalServerError_WhenExceptionThrown() {
        // Test : Vérifier la gestion des exceptions lors de la recherche d'utilisateur
        when(userDao.findByEmailOrUsername(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        ResponseEntity<String> response = userService.signUp(validRequestMap);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains(ReservationConstants.SOMETHING_WENT_WRONG));
    }

    @Test
    void signUp_ShouldReturnInternalServerError_WhenPasswordEncoderFails() {
        // Test : Vérifier la gestion des exceptions lors de l'encodage du mot de passe
        when(userDao.findByEmailOrUsername(anyString(), anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString()))
                .thenThrow(new RuntimeException("Password encoding failed"));

        ResponseEntity<String> response = userService.signUp(validRequestMap);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains(ReservationConstants.SOMETHING_WENT_WRONG));
    }

    // ==================== TESTS POUR LA MÉTHODE login() ====================

    @Test
    void login_ShouldReturnToken_WhenValidCredentialsWithEmail() {
        // Test : Vérifier qu'une connexion réussit avec un email valide
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("identifier", "john.doe@example.com");
        loginRequest.put("password", "password123");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("john.doe@example.com")
                .password("encodedPassword")
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))
                .build();

        when(customerUsersDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userDao.findByEmail("john.doe@example.com")).thenReturn(testUser);
        when(jwtUtil.generateToken("john.doe@example.com", "CLIENT")).thenReturn("jwt-token-123");

        ResponseEntity<String> response = userService.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"token\":\"jwt-token-123\""));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenMissingIdentifier() {
        // Test : Vérifier qu'une connexion échoue si l'identifiant est manquant
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("password", "password123");

        ResponseEntity<String> response = userService.login(loginRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Identifier and Password are required"));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenMissingPassword() {
        // Test : Vérifier qu'une connexion échoue si le mot de passe est manquant
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("identifier", "john.doe@example.com");

        ResponseEntity<String> response = userService.login(loginRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Identifier and Password are required"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenBadCredentials() {
        // Test : Vérifier qu'une connexion échoue avec de mauvaises informations d'identification
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("identifier", "john.doe@example.com");
        loginRequest.put("password", "wrongpassword");

        when(customerUsersDetailsService.loadUserByUsername("john.doe@example.com"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<String> response = userService.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Bad Credentials"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenUserNotFound() {
        // Test : Vérifier qu'une connexion échoue si l'utilisateur n'existe pas
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("identifier", "nonexistent@example.com");
        loginRequest.put("password", "password123");

        when(customerUsersDetailsService.loadUserByUsername("nonexistent@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        ResponseEntity<String> response = userService.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("User not found"));
    }

    @Test
    void login_ShouldReturnInternalServerError_WhenUnexpectedException() {
        // Test : Vérifier la gestion des exceptions inattendues lors de la connexion
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("identifier", "john.doe@example.com");
        loginRequest.put("password", "password123");

        when(customerUsersDetailsService.loadUserByUsername("john.doe@example.com"))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<String> response = userService.login(loginRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("An error occurred during authentication"));
    }

    // ==================== TESTS POUR LA MÉTHODE getAllClient() ====================

    @Test
    void getAllClient_ShouldReturnClients_WhenGlobalAdmin() {
        // Test : Vérifier qu'un administrateur global peut récupérer tous les clients
        List<UserWrapper> clients = Arrays.asList(
                new UserWrapper(1L, "John", "Doe", "johndoe", "john@example.com", "0123456789"),
                new UserWrapper(2L, "Jane", "Smith", "janesmith", "jane@example.com", "0987654321")
        );

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.getAllClient()).thenReturn(clients);

        ResponseEntity<List<UserWrapper>> response = userService.getAllClient();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("John", response.getBody().get(0).getNom());
        verify(userDao, times(1)).getAllClient();
    }

    @Test
    void getAllClient_ShouldReturnClients_WhenHotelAdmin() {
        // Test : Vérifier qu'un administrateur d'hôtel peut récupérer tous les clients
        List<UserWrapper> clients = Collections.singletonList(
                new UserWrapper(1L, "John", "Doe", "johndoe", "john@example.com", "0123456789")
        );

        when(jwtFilter.isGlobalAdmin()).thenReturn(false);
        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        when(userDao.getAllClient()).thenReturn(clients);

        ResponseEntity<List<UserWrapper>> response = userService.getAllClient();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(userDao, times(1)).getAllClient();
    }

    @Test
    void getAllClient_ShouldReturnUnauthorized_WhenNotAdmin() {
        // Test : Vérifier qu'un non-administrateur ne peut pas récupérer les clients
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);
        when(jwtFilter.isHotelAdmin()).thenReturn(false);

        ResponseEntity<List<UserWrapper>> response = userService.getAllClient();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(userDao, never()).getAllClient();
    }

    // ==================== TESTS POUR LA MÉTHODE checkToken() ====================

    @Test
    void checkToken_ShouldReturnTrue() {
        // Test : Vérifier que la vérification du token retourne toujours true
        ResponseEntity<String> response = userService.cheekToken();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("true"));
    }

    // ==================== TESTS POUR LA MÉTHODE changePassword() ====================

    @Test
    void changePassword_ShouldReturnSuccess_WhenValidOldPassword() {
        // Test : Vérifier qu'un changement de mot de passe réussit avec l'ancien mot de passe correct
        Map<String, String> passwordMap = new HashMap<>();
        passwordMap.put("oldPassword", "oldPassword123");
        passwordMap.put("newPassword", "newPassword123");

        when(jwtFilter.getCurrentUserRole()).thenReturn("john.doe@example.com");
        when(userDao.findByEmail("john.doe@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userDao.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<String> response = userService.changePassword(passwordMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Password updated successfully"));
        verify(userDao, times(1)).save(testUser);
    }

    @Test
    void changePassword_ShouldReturnSuccess_WhenUserFoundByUsername() {
        // Test : Vérifier qu'un changement de mot de passe réussit avec identification par nom d'utilisateur
        Map<String, String> passwordMap = new HashMap<>();
        passwordMap.put("oldPassword", "oldPassword123");
        passwordMap.put("newPassword", "newPassword123");

        when(jwtFilter.getCurrentUserRole()).thenReturn("johndoe");
        when(userDao.findByEmail("johndoe")).thenReturn(null);
        when(userDao.findByUserName("johndoe")).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userDao.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<String> response = userService.changePassword(passwordMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Password updated successfully"));
        verify(userDao, times(1)).save(any(User.class));
    }

    @Test
    void changePassword_ShouldReturnBadRequest_WhenIncorrectOldPassword() {
        // Test : Vérifier qu'un changement de mot de passe échoue avec l'ancien mot de passe incorrect
        Map<String, String> passwordMap = new HashMap<>();
        passwordMap.put("oldPassword", "wrongPassword");
        passwordMap.put("newPassword", "newPassword123");

        when(jwtFilter.getCurrentUserRole()).thenReturn("john.doe@example.com");
        when(userDao.findByEmail("john.doe@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        ResponseEntity<String> response = userService.changePassword(passwordMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Incorrect Old Password"));
        verify(userDao, never()).save(any(User.class));
    }

    // ==================== TESTS POUR LA MÉTHODE forgotPassword() ====================

    @Test
    void forgotPassword_ShouldReturnSuccess_WhenUserFoundByEmail() {
        // Test : Vérifier qu'une récupération de mot de passe réussit avec un email valide
        Map<String, String> requestMap = new HashMap<>();

        when(jwtFilter.getCurrentUserRole()).thenReturn("john.doe@example.com");
        when(userDao.findByEmail("john.doe@example.com")).thenReturn(testUser);

        ResponseEntity<String> response = userService.forgotPassword(requestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Check your mail for credentials"));
    }

    @Test
    void forgotPassword_ShouldReturnSuccess_WhenUserFoundByUsername() {
        // Test : Vérifier qu'une récupération de mot de passe réussit avec un nom d'utilisateur valide
        Map<String, String> requestMap = new HashMap<>();

        when(jwtFilter.getCurrentUserRole()).thenReturn("johndoe");
        when(userDao.findByEmail("johndoe")).thenReturn(null);
        when(userDao.findByUserName("johndoe")).thenReturn(testUser);

        ResponseEntity<String> response = userService.forgotPassword(requestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Check your mail for credentials"));
    }

    @Test
    void forgotPassword_ShouldReturnSuccess_WhenUserNotFound() {
        // Test : Vérifier qu'une récupération de mot de passe retourne un succès même si l'utilisateur n'existe pas (sécurité)
        Map<String, String> requestMap = new HashMap<>();

        when(jwtFilter.getCurrentUserRole()).thenReturn("nonexistent@example.com");
        when(userDao.findByEmail("nonexistent@example.com")).thenReturn(null);
        when(userDao.findByUserName("nonexistent@example.com")).thenReturn(null);

        ResponseEntity<String> response = userService.forgotPassword(requestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Check your mail for credentials"));
    }

    @Test
    void forgotPassword_ShouldReturnSuccess_WhenUserEmailIsEmpty() {
        // Test : Vérifier qu'une récupération de mot de passe ne déclenche pas d'email si l'email est vide
        Map<String, String> requestMap = new HashMap<>();
        testUser.setEmail("");

        when(jwtFilter.getCurrentUserRole()).thenReturn("john.doe@example.com");
        when(userDao.findByEmail("john.doe@example.com")).thenReturn(testUser);

        ResponseEntity<String> response = userService.forgotPassword(requestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Check your mail for credentials"));
    }
}