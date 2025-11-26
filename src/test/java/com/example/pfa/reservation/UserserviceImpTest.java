package com.example.pfa.reservation;

import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.service.impl.UserserviceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserserviceImpTest {

    @InjectMocks
    private UserserviceImp userservice;

    @Mock
    private UserDAO userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------- TEST SIGNUP SUCCESS ----------------
    @Test
    void testSignUp_Success() {
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("nom", "Karima");
        requestMap.put("prenom", "El Maati");
        requestMap.put("email", "karima@test.com");
        requestMap.put("userName", "karima123");
        requestMap.put("password", "123456");
        requestMap.put("telephone", "0600000000");
        requestMap.put("role", "CLIENT");

        // Simule qu'aucun utilisateur existant
        when(userDao.findByEmailOrUsername("karima@test.com", "karima123")).thenReturn(null);

        ResponseEntity<String> response = userservice.signUp(requestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully Registered"));

        // Vérifie que save a bien été appelé
        verify(userDao, times(1)).save(any(User.class));
    }

    // ---------------- TEST SIGNUP EMAIL ALREADY EXISTS ----------------
    @Test
    void testSignUp_EmailAlreadyExists() {
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("nom", "Karima");
        requestMap.put("prenom", "El Maati");
        requestMap.put("email", "karima@test.com");
        requestMap.put("userName", "karima123");
        requestMap.put("password", "123456");
        requestMap.put("telephone", "0600000000");

        // Simule un utilisateur existant avec même email
        User existingUser = new User();
        existingUser.setEmail("karima@test.com");
        existingUser.setUserName("autreUser");

        when(userDao.findByEmailOrUsername("karima@test.com", "karima123")).thenReturn(existingUser);

        ResponseEntity<String> response = userservice.signUp(requestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Email already exists"));

        // Vérifie que userDao.save n'a pas été appelé
        verify(userDao, never()).save(any(User.class));
    }

    // ---------------- TEST CHECK TOKEN ----------------
    @Test
    void testCheekToken() {
        ResponseEntity<String> response = userservice.cheekToken();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("true"));
    }
}
