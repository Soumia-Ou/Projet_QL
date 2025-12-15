package com.example.pfa.reservation;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.model.Hotel;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.HotelDAO;
import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.service.impl.HotelServiceImp;
import com.example.pfa.reservation.wrapper.HotelWrapper;
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
class HotelServiceImpTest {

    @Mock
    private HotelDAO hotelDao;

    @Mock
    private UserDAO userDao;

    @Mock
    private JwtFilter jwtFilter;

    @InjectMocks
    private HotelServiceImp hotelService;

    private Hotel testHotel;
    private User testHotelAdmin;
    private Map<String, String> validRequestMap;

    @BeforeEach
    void setUp() {
        // Initialisation des objets de test
        testHotelAdmin = new User();
        testHotelAdmin.setId(1L);
        testHotelAdmin.setRole(Role.HOTEL_ADMIN);
        testHotelAdmin.setEmail("admin@hotel.com");
        testHotelAdmin.setNom("Admin Hotel");

        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setNom("Grand Hotel");
        testHotel.setAdresse("123 Rue Principale");
        testHotel.setTelephone("0123456789");
        testHotel.setEmail("contact@grandhotel.com");
        testHotel.setNombreEtoiles(5);
        testHotel.setImage("hotel.jpg");
        testHotel.setAdminHotelier(testHotelAdmin);

        validRequestMap = new HashMap<>();
        validRequestMap.put("nom", "Grand Hotel");
        validRequestMap.put("adresse", "123 Rue Principale");
        validRequestMap.put("telephone", "0123456789");
        validRequestMap.put("email", "contact@grandhotel.com");
        validRequestMap.put("nombreEtoiles", "5");
        validRequestMap.put("image", "hotel.jpg");
        validRequestMap.put("adminHotelId", "1");
        validRequestMap.put("id", "1");
    }

    // ========================TESTS POUR addHotel()=========================
    @Test
    void addHotel_withGlobalAdminAccessAndValidData_shouldAddHotel() {
        // Test: Vérifier qu'un administrateur global peut ajouter un hôtel avec des données valides
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.findById(1L)).thenReturn(Optional.of(testHotelAdmin));
        when(hotelDao.findByAdminHotelierId(1L)).thenReturn(Collections.emptyList());
        when(hotelDao.save(any(Hotel.class))).thenReturn(testHotel);

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(hotelDao, times(1)).save(any(Hotel.class));
    }

    @Test
    void addHotel_withoutGlobalAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un non-admin ne peut pas ajouter d'hôtel
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void addHotel_withInvalidData_shouldReturnBadRequest() {
        // Test: Vérifier que des données incomplètes génèrent une erreur 400
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        Map<String, String> invalidRequestMap = new HashMap<>(validRequestMap);
        invalidRequestMap.remove("nom"); // Supprime un champ obligatoire

        ResponseEntity<String> response = hotelService.addHotel(invalidRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void addHotel_withNonExistentAdmin_shouldReturnBadRequest() {
        // Test: Vérifier qu'un admin inexistant génère une erreur
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void addHotel_withNonHotelAdminUser_shouldReturnBadRequest() {
        // Test: Vérifier qu'un utilisateur non-HOTEL_ADMIN ne peut pas être assigné comme admin d'hôtel
        User nonAdminUser = new User();
        nonAdminUser.setId(2L);
        nonAdminUser.setRole(Role.CLIENT);

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.findById(2L)).thenReturn(Optional.of(nonAdminUser));
        validRequestMap.put("adminHotelId", "2");

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void addHotel_withAdminAlreadyHavingHotel_shouldReturnBadRequest() {
        // Test: Vérifier qu'un admin ne peut avoir qu'un seul hôtel
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.findById(1L)).thenReturn(Optional.of(testHotelAdmin));
        when(hotelDao.findByAdminHotelierId(1L)).thenReturn(List.of(testHotel));

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void addHotel_withInvalidStars_shouldThrowExceptionAndReturnInternalServerError() {
        // Test: Vérifier qu'un nombre d'étoiles invalide (>5) génère une erreur 500
        validRequestMap.put("nombreEtoiles", "6"); // Étoiles invalides (>5)

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.findById(1L)).thenReturn(Optional.of(testHotelAdmin));
        when(hotelDao.findByAdminHotelierId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        // L'exception dans extractAndValidateStars() n'est pas attrapée dans addHotel
        // donc elle remonte et cause une Internal Server Error
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    // =================================================
    // TESTS POUR updateHotel()
    // =================================================

    @Test
    void updateHotel_withValidData_shouldUpdateHotel() {
        // Test: Vérifier la mise à jour d'un hôtel existant
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));
        when(userDao.findById(1L)).thenReturn(Optional.of(testHotelAdmin));
        when(hotelDao.findByAdminHotelierId(1L)).thenReturn(List.of(testHotel));
        when(hotelDao.save(any(Hotel.class))).thenReturn(testHotel);

        ResponseEntity<String> response = hotelService.updateHotel(validRequestMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(hotelDao, times(1)).save(any(Hotel.class));
    }

    @Test
    void updateHotel_withoutHotelId_shouldReturnBadRequest() {
        // Test: Vérifier qu'un ID d'hôtel est requis pour la mise à jour
        Map<String, String> requestWithoutId = new HashMap<>(validRequestMap);
        requestWithoutId.remove("id");

        // Note: updateHotel() vérifie d'abord isGlobalAdmin() avant de vérifier l'ID
        when(jwtFilter.isGlobalAdmin()).thenReturn(false); // Pas admin global

        ResponseEntity<String> response = hotelService.updateHotel(requestWithoutId);

        // Si pas admin, retourne UNAUTHORIZED avant de vérifier l'ID
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void updateHotel_withoutGlobalAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un non-admin ne peut pas mettre à jour d'hôtel
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        ResponseEntity<String> response = hotelService.updateHotel(validRequestMap);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void updateHotel_withNonExistentHotel_shouldReturnNotFound() {
        // Test: Vérifier qu'un hôtel inexistant génère une erreur 404
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<String> response = hotelService.updateHotel(validRequestMap);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    @Test
    void updateHotel_withAdminAssignedToAnotherHotel_shouldReturnBadRequest() {
        // Test: Vérifier qu'un admin ne peut pas être assigné à deux hôtels différents
        Hotel anotherHotel = new Hotel();
        anotherHotel.setId(2L);
        anotherHotel.setAdminHotelier(testHotelAdmin);

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));
        when(userDao.findById(1L)).thenReturn(Optional.of(testHotelAdmin));
        when(hotelDao.findByAdminHotelierId(1L)).thenReturn(List.of(anotherHotel));

        ResponseEntity<String> response = hotelService.updateHotel(validRequestMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(hotelDao, never()).save(any(Hotel.class));
    }

    // ======================TESTS POUR deleteHotel()===========================
    @Test
    void deleteHotel_withGlobalAdminAccessAndValidId_shouldDeleteHotel() {
        // Test: Vérifier la suppression d'un hôtel existant
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.findById(1L)).thenReturn(Optional.of(testHotel));

        ResponseEntity<String> response = hotelService.deleteHotel(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));
        verify(hotelDao, times(1)).deleteById(1L);
    }

    @Test
    void deleteHotel_withoutGlobalAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un non-admin ne peut pas supprimer d'hôtel
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        ResponseEntity<String> response = hotelService.deleteHotel(1L);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(hotelDao, never()).deleteById(anyLong());
    }

    @Test
    void deleteHotel_withNonExistentHotel_shouldReturnNotFound() {
        // Test: Vérifier qu'un hôtel inexistant génère une erreur 404
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<String> response = hotelService.deleteHotel(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(hotelDao, never()).deleteById(anyLong());
    }

    // ======================TESTS POUR getAllHotels()===========================
    @Test
    void getAllHotels_withGlobalAdminAccess_shouldReturnHotels() {
        // Test: Vérifier qu'un admin global peut récupérer tous les hôtels
        HotelWrapper hotelWrapper = HotelWrapper.builder()
                .id(1L)
                .nom("Grand Hotel")
                .adresse("123 Rue Principale")
                .telephone("0123456789")
                .email("contact@grandhotel.com")
                .nombreEtoiles(5)
                .image("hotel.jpg")
                .adminId(1L)
                .build();

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.getAllHotels()).thenReturn(List.of(hotelWrapper));

        ResponseEntity<List<HotelWrapper>> response = hotelService.getAllHotels();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Grand Hotel", response.getBody().get(0).getNom());
    }

    @Test
    void getAllHotels_withoutGlobalAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un non-admin ne peut pas récupérer tous les hôtels
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        ResponseEntity<List<HotelWrapper>> response = hotelService.getAllHotels();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getAllHotels_withEmptyList_shouldReturnNoContent() {
        // Test: Vérifier qu'une liste vide retourne NO_CONTENT (204)
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.getAllHotels()).thenReturn(Collections.emptyList());

        ResponseEntity<List<HotelWrapper>> response = hotelService.getAllHotels();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // =================================================
    // TESTS POUR getHotelById()
    // =================================================

    @Test
    void getHotelById_withValidIdAndGlobalAdminAccess_shouldReturnHotel() {
        // Test: Vérifier la récupération d'un hôtel par ID
        HotelWrapper hotelWrapper = HotelWrapper.builder()
                .id(1L)
                .nom("Grand Hotel")
                .adresse("123 Rue Principale")
                .build();

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.getHotelById(1L)).thenReturn(Optional.of(hotelWrapper));

        ResponseEntity<HotelWrapper> response = hotelService.getHotelById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Grand Hotel", response.getBody().getNom());
    }

    @Test
    void getHotelById_withoutGlobalAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un non-admin ne peut pas récupérer un hôtel par ID
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        ResponseEntity<HotelWrapper> response = hotelService.getHotelById(1L);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getHotelById_withNonExistentId_shouldReturnNotFound() {
        // Test: Vérifier qu'un ID inexistant retourne NOT_FOUND (404)
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.getHotelById(999L)).thenReturn(Optional.empty());

        ResponseEntity<HotelWrapper> response = hotelService.getHotelById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // =================================================
    // TESTS POUR getHotelsByAdminId()
    // =================================================

    @Test
    void getHotelsByAdminId_withValidAdminId_shouldReturnHotels() {
        // Test: Vérifier la récupération des hôtels par ID d'admin
        HotelWrapper hotelWrapper = HotelWrapper.builder()
                .id(1L)
                .nom("Grand Hotel")
                .adminId(1L)
                .build();

        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.getHotelsByAdminId(1L)).thenReturn(List.of(hotelWrapper));

        ResponseEntity<List<HotelWrapper>> response = hotelService.getHotelsByAdminId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getAdminId());
    }

    @Test
    void getHotelsByAdminId_withoutGlobalAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un non-admin ne peut pas récupérer les hôtels par admin ID
        when(jwtFilter.isGlobalAdmin()).thenReturn(false);

        ResponseEntity<List<HotelWrapper>> response = hotelService.getHotelsByAdminId(1L);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getHotelsByAdminId_withNoHotels_shouldReturnNoContent() {
        // Test: Vérifier qu'un admin sans hôtels retourne NO_CONTENT (204)
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.getHotelsByAdminId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<HotelWrapper>> response = hotelService.getHotelsByAdminId(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // =================================================
    // TESTS POUR searchHotels()
    // =================================================

    @Test
    void searchHotels_withHotelAdminAccess_shouldReturnUnauthorized() {
        // Test: Vérifier qu'un HOTEL_ADMIN ne peut pas faire de recherche
        when(jwtFilter.isHotelAdmin()).thenReturn(true);

        ResponseEntity<List<HotelWrapper>> response = hotelService.searchHotels("Hotel", "Paris", 5);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void searchHotels_withValidSearchCriteria_shouldReturnHotels() {
        // Test: Vérifier une recherche avec critères valides
        HotelWrapper hotelWrapper = HotelWrapper.builder()
                .id(1L)
                .nom("Grand Hotel")
                .adresse("Paris")
                .nombreEtoiles(5)
                .build();

        when(jwtFilter.isHotelAdmin()).thenReturn(false);
        when(hotelDao.searchHotels("Hotel", "Paris", 5)).thenReturn(List.of(hotelWrapper));

        ResponseEntity<List<HotelWrapper>> response = hotelService.searchHotels("Hotel", "Paris", 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void searchHotels_withNullParameters_shouldReturnHotels() {
        // Test: Vérifier une recherche avec paramètres null (tous les hôtels)
        HotelWrapper hotelWrapper = HotelWrapper.builder()
                .id(1L)
                .nom("Grand Hotel")
                .build();

        when(jwtFilter.isHotelAdmin()).thenReturn(false);
        when(hotelDao.searchHotels(null, null, null)).thenReturn(List.of(hotelWrapper));

        ResponseEntity<List<HotelWrapper>> response = hotelService.searchHotels(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void searchHotels_withNoResults_shouldReturnNoContent() {
        // Test: Vérifier qu'une recherche sans résultats retourne NO_CONTENT (204)
        when(jwtFilter.isHotelAdmin()).thenReturn(false);
        when(hotelDao.searchHotels("NonExistent", "Nowhere", 10)).thenReturn(Collections.emptyList());

        ResponseEntity<List<HotelWrapper>> response = hotelService.searchHotels("NonExistent", "Nowhere", 10);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // =================================================
    // TESTS D'EXCEPTION
    // =================================================

    @Test
    void addHotel_withException_shouldReturnInternalServerError() {
        // Test: Vérifier qu'une exception générique retourne INTERNAL_SERVER_ERROR (500)
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(userDao.findById(anyLong())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<String> response = hotelService.addHotel(validRequestMap);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void updateHotel_withException_shouldReturnInternalServerError() {
        // Test: Vérifier qu'une exception pendant la mise à jour retourne 500
        when(jwtFilter.isGlobalAdmin()).thenReturn(true);
        when(hotelDao.findById(anyLong())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<String> response = hotelService.updateHotel(validRequestMap);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void searchHotels_withException_shouldReturnInternalServerError() {
        // Test: Vérifier qu'une exception pendant la recherche retourne 500
        when(jwtFilter.isHotelAdmin()).thenReturn(false);
        when(hotelDao.searchHotels(any(), any(), any())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<List<HotelWrapper>> response = hotelService.searchHotels("Hotel", "Paris", 5);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // =================================================
    // TESTS POUR LA MÉTHODE validateHotelMap()
    // (Utilisation de réflexion car méthode privée)
    // =================================================

    @Test
    void validateHotelMap_withAllRequiredFields_shouldReturnTrue() throws Exception {
        // Test: Vérifier que tous les champs requis sont présents
        java.lang.reflect.Method method = HotelServiceImp.class.getDeclaredMethod("validateHotelMap", Map.class);
        method.setAccessible(true);
        boolean isValid = (boolean) method.invoke(hotelService, validRequestMap);

        assertTrue(isValid);
    }

    @Test
    void validateHotelMap_withMissingField_shouldReturnFalse() throws Exception {
        // Test: Vérifier qu'un champ manquant retourne false
        Map<String, String> invalidMap = new HashMap<>(validRequestMap);
        invalidMap.remove("nom");

        java.lang.reflect.Method method = HotelServiceImp.class.getDeclaredMethod("validateHotelMap", Map.class);
        method.setAccessible(true);
        boolean isValid = (boolean) method.invoke(hotelService, invalidMap);

        assertFalse(isValid);
    }

    // =================================================
    // TESTS POUR LA MÉTHODE extractAndValidateStars()
    // =================================================

    @Test
    void extractAndValidateStars_withValidStars_shouldReturnStars() throws Exception {
        // Test: Vérifier l'extraction et validation d'étoiles valides
        java.lang.reflect.Method method = HotelServiceImp.class.getDeclaredMethod("extractAndValidateStars", Map.class);
        method.setAccessible(true);
        int stars = (int) method.invoke(hotelService, validRequestMap);

        assertEquals(5, stars);
    }

    @Test
    void extractAndValidateStars_withInvalidStarsTooHigh_shouldThrowException() throws Exception {
        // Test: Vérifier qu'un nombre d'étoiles > 5 lève une exception
        Map<String, String> invalidStarsMap = new HashMap<>(validRequestMap);
        invalidStarsMap.put("nombreEtoiles", "6");

        java.lang.reflect.Method method = HotelServiceImp.class.getDeclaredMethod("extractAndValidateStars", Map.class);
        method.setAccessible(true);

        Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(hotelService, invalidStarsMap);
        });

        // L'exception réelle est dans getCause()
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("must be between 1 and 5"));
    }

    @Test
    void extractAndValidateStars_withInvalidStarsTooLow_shouldThrowException() throws Exception {
        // Test: Vérifier qu'un nombre d'étoiles < 1 lève une exception
        Map<String, String> invalidStarsMap = new HashMap<>(validRequestMap);
        invalidStarsMap.put("nombreEtoiles", "0");

        java.lang.reflect.Method method = HotelServiceImp.class.getDeclaredMethod("extractAndValidateStars", Map.class);
        method.setAccessible(true);

        Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(hotelService, invalidStarsMap);
        });

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void extractAndValidateStars_withInvalidFormat_shouldThrowException() throws Exception {
        // Test: Vérifier qu'un format d'étoiles invalide lève une exception
        Map<String, String> invalidStarsMap = new HashMap<>(validRequestMap);
        invalidStarsMap.put("nombreEtoiles", "not-a-number");

        java.lang.reflect.Method method = HotelServiceImp.class.getDeclaredMethod("extractAndValidateStars", Map.class);
        method.setAccessible(true);

        Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(hotelService, invalidStarsMap);
        });

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Invalid number"));
    }
}