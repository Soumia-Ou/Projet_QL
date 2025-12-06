package com.example.pfa.reservation.service.impl;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.repository.HotelDAO;
import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.model.Hotel;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.service.HotelService;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.HotelWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class HotelServiceImp implements HotelService {

    private final HotelDAO hotelDao;
    private final UserDAO userDao;
    private final JwtFilter jwtFilter;

    // Constantes pour les clés du map
    private static final String NOM = "nom";
    private static final String ADRESSE = "adresse";
    private static final String TELEPHONE = "telephone";
    private static final String EMAIL = "email";
    private static final String IMAGE = "image";
    private static final String NOMBRE_ETOILES = "nombreEtoiles";
    private static final String ADMIN_HOTEL_ID = "adminHotelId";
    private static final String ID = "id";

    public HotelServiceImp(HotelDAO hotelDao, UserDAO userDao, JwtFilter jwtFilter) {
        this.hotelDao = hotelDao;
        this.userDao = userDao;
        this.jwtFilter = jwtFilter;
    }

    @Override
    public ResponseEntity<String> addHotel(Map<String, String> requestMap) {
        try {
            // Vérifie que l'utilisateur est administrateur global
            if (!jwtFilter.isGlobalAdmin()) {
                return ReservationUtils.getResponseEntity(
                        ReservationConstants.UNAUTHORIZED_ACCESS,
                        HttpStatus.UNAUTHORIZED
                );
            }

            // Vérifie la validité des données reçues
            if (!validateHotelMap(requestMap)) {
                return ReservationUtils.getResponseEntity(
                        ReservationConstants.INVALID_DATA,
                        HttpStatus.BAD_REQUEST
                );
            }

            // Récupération de l'ID de l'administrateur de l'hôtel
            String adminIdStr = requestMap.get(ADMIN_HOTEL_ID);
            if (adminIdStr == null) {
                return ReservationUtils.getResponseEntity(
                        "Admin hotel ID is missing",
                        HttpStatus.BAD_REQUEST
                );
            }

            Long adminId;
            try {
                adminId = Long.parseLong(adminIdStr);
            } catch (NumberFormatException e) {
                return ReservationUtils.getResponseEntity(
                        "Invalid admin hotel ID format",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Récupération de l'utilisateur administrateur
            User admin = userDao.findById(adminId).orElse(null);
            if (admin == null || admin.getRole() != Role.HOTEL_ADMIN) {
                return ReservationUtils.getResponseEntity(
                        "Invalid hotel admin",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Vérifie si l'admin n'a pas déjà un hôtel
            List<Hotel> existingHotels = hotelDao.findByAdminHotelierId(adminId);
            if (!existingHotels.isEmpty()) {
                return ReservationUtils.getResponseEntity(
                        "Admin already has a hotel",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Création du nouvel hôtel
            Hotel hotel = new Hotel();
            hotel.setNom(requestMap.get(NOM));
            hotel.setAdresse(requestMap.get(ADRESSE));
            hotel.setTelephone(requestMap.get(TELEPHONE));
            hotel.setEmail(requestMap.get(EMAIL));
            hotel.setImage(requestMap.get(IMAGE));
            hotel.setNombreEtoiles(extractAndValidateStars(requestMap));
            hotel.setAdminHotelier(admin);

            hotelDao.save(hotel);

            return ReservationUtils.getResponseEntity(
                    "Hotel added successfully",
                    HttpStatus.OK
            );

        } catch (Exception ex) {
            log.error("Error in addHotel: {}", ex.getMessage(), ex);
            return ReservationUtils.getResponseEntity(
                    ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    @Override
    public ResponseEntity<String> updateHotel(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!requestMap.containsKey(ID)) {
                return ReservationUtils.getResponseEntity("Hotel ID is required", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get(ID));
            Optional<Hotel> optionalHotel = hotelDao.findById(hotelId);
            if (optionalHotel.isEmpty()) {
                return ReservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            if (!validateHotelMap(requestMap)) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            Hotel hotel = optionalHotel.get();
            hotel.setNom(requestMap.get(NOM));
            hotel.setAdresse(requestMap.get(ADRESSE));
            hotel.setTelephone(requestMap.get(TELEPHONE));
            hotel.setEmail(requestMap.get(EMAIL));
            hotel.setImage(requestMap.get(IMAGE));
            hotel.setNombreEtoiles(extractAndValidateStars(requestMap));

            Long adminId = Long.parseLong(requestMap.get(ADMIN_HOTEL_ID));
            User admin = userDao.findById(adminId).orElse(null);
            if (admin == null || !"HOTEL_ADMIN".equals(admin.getRole().toString())) {
                return ReservationUtils.getResponseEntity("Invalid hotel admin", HttpStatus.BAD_REQUEST);
            }

            // Vérifie que l'admin n'est pas déjà assigné à un autre hôtel
            boolean isAssignedToAnotherHotel = hotelDao.findByAdminHotelierId(adminId).stream()
                    .anyMatch(h -> !h.getId().equals(hotelId));
            if (isAssignedToAnotherHotel) {
                return ReservationUtils.getResponseEntity("This admin is already assigned to another hotel", HttpStatus.BAD_REQUEST);
            }

            hotel.setAdminHotelier(admin);
            hotelDao.save(hotel);

            return ReservationUtils.getResponseEntity("Hotel updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error in updateHotel: {}", ex.getMessage());
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> deleteHotel(Long id) {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!hotelDao.findById(id).isPresent()) {
                return ReservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            hotelDao.deleteById(id);
            return ReservationUtils.getResponseEntity("Hotel deleted successfully", HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error in deleteHotel: {}", ex.getMessage());
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> getAllHotels() {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
            }

            List<HotelWrapper> hotels = hotelDao.getAllHotels();
            if (hotels.isEmpty()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(hotels, HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error in getAllHotels: {}", ex.getMessage());
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<HotelWrapper> getHotelById(Long id) {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            Optional<HotelWrapper> optionalHotel = hotelDao.getHotelById(id);
            return optionalHotel
                    .map(hotelWrapper -> new ResponseEntity<>(hotelWrapper, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        } catch (Exception ex) {
            log.error("Error in getHotelById: {}", ex.getMessage());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> getHotelsByAdminId(Long adminId) {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
            }

            List<HotelWrapper> hotels = hotelDao.getHotelsByAdminId(adminId);
            if (hotels.isEmpty()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(hotels, HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error in getHotelsByAdminId: {}", ex.getMessage());
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> searchHotels(String nom, String adresse, Integer etoiles) {
        try {
            if (jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
            }

            List<HotelWrapper> hotels = hotelDao.searchHotels(nom, adresse, etoiles);
            if (hotels.isEmpty()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(hotels, HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error in searchHotels: {}", ex.getMessage());
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Validation des champs obligatoires
    public boolean validateHotelMap(Map<String, String> requestMap) {
        return requestMap.containsKey(NOM) &&
                requestMap.containsKey(ADRESSE) &&
                requestMap.containsKey(TELEPHONE) &&
                requestMap.containsKey(EMAIL) &&
                requestMap.containsKey(NOMBRE_ETOILES) &&
                requestMap.containsKey(IMAGE) &&
                requestMap.containsKey(ADMIN_HOTEL_ID);
    }

    // Extraction et validation du nombre d'étoiles
    public int extractAndValidateStars(Map<String, String> requestMap) {
        try {
            int etoiles = Integer.parseInt(requestMap.get(NOMBRE_ETOILES));
            if (etoiles < 1 || etoiles > 5) {
                throw new IllegalArgumentException("Number of stars must be between 1 and 5");
            }
            return etoiles;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number of stars", ex);
        }
    }
}
