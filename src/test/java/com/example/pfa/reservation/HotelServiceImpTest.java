package com.example.pfa.reservation;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.model.Hotel;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.HotelDAO;
import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.service.impl.HotelServiceImp;
import com.example.pfa.reservation.utils.ReservationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotelServiceImpTest {

    @InjectMocks
    private HotelServiceImp hotelService;

    @Mock
    private HotelDAO hotelDao;

    @Mock
    private UserDAO userDao;

    @Mock
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void testAddHotel_InvalidData() {
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);

        Map<String, String> requestMap = new HashMap<>();
        ResponseEntity<String> response = hotelService.addHotel(requestMap);

        // Vérifie que le JSON contient le message attendu
        assertTrue(response.getBody().contains("Invalid Data"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testAddHotel_Unauthorized() {
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        Map<String, String> requestMap = new HashMap<>();
        ResponseEntity<String> response = hotelService.addHotel(requestMap);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
