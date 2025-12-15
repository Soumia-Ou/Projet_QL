package com.example.pfa.reservation;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.model.Chambre;
import com.example.pfa.reservation.model.Hotel;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.ChambreDAO;
import com.example.pfa.reservation.repository.HotelDAO;
import com.example.pfa.reservation.service.impl.ChambreServiceImp;
import com.example.pfa.reservation.wrapper.ChambreWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChambreServiceImpTest {

    @Mock
    private ChambreDAO chambreDao;

    @Mock
    private HotelDAO hotelDao;

    @Mock
    private JwtFilter jwtFilter;

    @InjectMocks
    private ChambreServiceImp chambreService;

    private Hotel testHotel;
    private Chambre testChambre;
    private User testHotelAdmin;
    private Map<String, String> validRequestMap;

    @BeforeEach
    void setUp() {
        testHotelAdmin = new User();
        testHotelAdmin.setId(1L);
        testHotelAdmin.setRole(Role.HOTEL_ADMIN);
        testHotelAdmin.setEmail("admin@hotel.com");
        testHotelAdmin.setUserName("hoteladmin");

        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setNom("Grand Hotel");
        testHotel.setAdminHotelier(testHotelAdmin);

        testChambre = new Chambre();
        testChambre.setId(1L);
        testChambre.setNumero("101");
        testChambre.setTypeChambre("Deluxe");
        testChambre.setPrix(150.0);
        testChambre.setDisponibilite(true);
        testChambre.setHotel(testHotel);

        validRequestMap = new HashMap<>();
        validRequestMap.put("numero", "101");
        validRequestMap.put("typeChambre", "Deluxe");
        validRequestMap.put("prix", "150.0");
        validRequestMap.put("disponibilite", "true");
        validRequestMap.put("hotelId", "1");
        validRequestMap.put("id", "1");
    }

    // =====================TESTS POUR addChambre() ==========================
    @Test
    void addChambre_withHotelAdminAccessAndValidData_shouldAddChambre() {
        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        when(jwtFilter.getCurrentUserRole()).thenReturn("admin@hotel.com");

        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));
        when(chambreDao.save(any(Chambre.class))).thenReturn(testChambre);

        ResponseEntity<String> response = chambreService.addChambre(validRequestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(chambreDao, times(1)).save(any(Chambre.class));
    }
    @Test
    void addChambre_withoutAdminAccess_shouldReturnUnauthorized() {
        when(jwtFilter.isHotelAdmin()).thenReturn(false);

        ResponseEntity<String> response = chambreService.addChambre(validRequestMap);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(chambreDao, never()).save(any(Chambre.class));
        verify(hotelDao, never()).findById(anyLong());
    }

    @Test
    void addChambre_withInvalidInput_shouldReturnBadRequest() {
        validRequestMap.put("prix", "invalid-price");

        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        // getCurrentUserRole n'est pas appelé pour les données invalides

        ResponseEntity<String> response = chambreService.addChambre(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(chambreDao, never()).save(any(Chambre.class));
        verify(hotelDao, never()).findById(anyLong());
    }

    @Test
    void addChambre_withNonExistentHotel_shouldReturnNotFound() {
        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        // getCurrentUserRole n'est pas appelé si l'hôtel n'existe pas

        when(hotelDao.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<String> response = chambreService.addChambre(validRequestMap);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(chambreDao, never()).save(any(Chambre.class));
    }

    @Test
    void addChambre_withHotelAdminAccessingOtherHotel_shouldReturnUnauthorized() {
        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        when(jwtFilter.getCurrentUserRole()).thenReturn("other@hotel.com");

        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));

        ResponseEntity<String> response = chambreService.addChambre(validRequestMap);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(chambreDao, never()).save(any(Chambre.class));
    }

    // =====================TESTS POUR updateChambre()===========================
    @Test
    void updateChambre_withValidDataAndHotelAdmin_shouldUpdateChambre() {
        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        when(jwtFilter.getCurrentUserRole()).thenReturn("admin@hotel.com");

        when(chambreDao.findById(1L)).thenReturn(Optional.of(testChambre));
        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));
        when(chambreDao.save(any(Chambre.class))).thenReturn(testChambre);

        ResponseEntity<String> response = chambreService.updateChambre(validRequestMap);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(chambreDao, times(1)).save(any(Chambre.class));
    }
    @Test
    void updateChambre_withoutRoomId_shouldReturnBadRequest() {
        Map<String, String> requestWithoutId = new HashMap<>(validRequestMap);
        requestWithoutId.remove("id");

        when(jwtFilter.isHotelAdmin()).thenReturn(true);

        ResponseEntity<String> response = chambreService.updateChambre(requestWithoutId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(chambreDao, never()).save(any(Chambre.class));
    }

    @Test
    void updateChambre_withNonExistentRoom_shouldReturnNotFound() {
        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        // getCurrentUserRole n'est pas appelé si la chambre n'existe pas

        when(chambreDao.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<String> response = chambreService.updateChambre(validRequestMap);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(chambreDao, never()).save(any(Chambre.class));
        verify(hotelDao, never()).findById(anyLong());
    }

    // =================================================
    // TESTS POUR getAvailableChambresByHotelId() - CORRIGÉS
    // =================================================

    @Test
    void getAvailableChambresByHotelId_withClientAccess_shouldReturnAvailableChambres() {
        ChambreWrapper wrapper = ChambreWrapper.builder()
                .id(1L)
                .numero("101")
                .disponibilite(true)
                .hotelId(1L)
                .build();

        when(jwtFilter.isClient()).thenReturn(true);
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);
        when(jwtFilter.isHotelAdmin()).thenReturn(false);

        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));
        when(chambreDao.getAvailableChambresByHotelId(1L)).thenReturn(List.of(wrapper));

        ResponseEntity<List<ChambreWrapper>> response =
                chambreService.getAvailableChambresByHotelId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().get(0).getDisponibilite());
    }

    @Test
    void getAvailableChambresByHotelId_withoutAccess_shouldReturnUnauthorized() {
        when(jwtFilter.isClient()).thenReturn(false);
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);
        when(jwtFilter.isHotelAdmin()).thenReturn(false);

        ResponseEntity<List<ChambreWrapper>> response =
                chambreService.getAvailableChambresByHotelId(1L);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        // Le corps peut être null ou une liste vide selon l'implémentation
        // Vérifiez ce que retourne réellement votre service
        if (response.getBody() != null) {
            assertTrue(response.getBody().isEmpty());
        }
    }

    @Test
    void getAvailableChambresByHotelId_withHotelAdminAccessingOwnHotel_shouldReturnChambres() {
        ChambreWrapper wrapper = ChambreWrapper.builder()
                .id(1L)
                .numero("101")
                .disponibilite(true)
                .hotelId(1L)
                .build();

        when(jwtFilter.isHotelAdmin()).thenReturn(true);
        when(jwtFilter.getCurrentUserRole()).thenReturn("admin@hotel.com");

        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));
        when(chambreDao.getAvailableChambresByHotelId(1L)).thenReturn(List.of(wrapper));

        ResponseEntity<List<ChambreWrapper>> response =
                chambreService.getAvailableChambresByHotelId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}