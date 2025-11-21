package com.example.pfa.reservation.serviceImp;

import com.example.pfa.reservation.JWT.JwtFilter;
import com.example.pfa.reservation.JWT.JwtUtil;
import com.example.pfa.reservation.Repository.hotelDAO;
import com.example.pfa.reservation.Repository.userDAO;
import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.model.hotel;
import com.example.pfa.reservation.model.user;
import com.example.pfa.reservation.service.hotelService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.HotelWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j //logging propre et efficace
@Service
public class hotelServiceImp implements hotelService {

    @Autowired
    private hotelDAO hotelDao;

    @Autowired
    userDAO userDao;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    JwtFilter jwtFilter;

    @Override
    public ResponseEntity<String> addHotel(Map<String, String> requestMap) {
        try {
            // Only GLOBAL_ADMIN can add hotels
            if (!jwtFilter.isGlobalAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Validate required fields
            if (!validateHotelMap(requestMap)) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            // Create hotel entity
            hotel hotel = new hotel();
            hotel.setNom(requestMap.get("nom"));
            hotel.setAdresse(requestMap.get("adresse"));
            hotel.setTelephone(requestMap.get("telephone"));
            hotel.setEmail(requestMap.get("email"));
            hotel.setImage(requestMap.get("image"));

            try {
                int etoiles = Integer.parseInt(requestMap.get("nombreEtoiles"));
                if (etoiles < 1 || etoiles > 5) {
                    return reservationUtils.getResponseEntity("Number of stars must be between 1 and 5", HttpStatus.BAD_REQUEST);
                }
                hotel.setNombreEtoiles(Integer.parseInt(requestMap.get("nombreEtoiles")));
            } catch (NumberFormatException ex) {
                return reservationUtils.getResponseEntity("Invalid number of stars", HttpStatus.BAD_REQUEST);
            }

            // Check the hotel admin (must be a user with the HOTEL_ADMIN role)
            Long adminId = Long.parseLong(requestMap.get("adminHotelId"));
            Optional<user> adminOpt = userDao.findById(adminId);

            if (adminOpt.isEmpty() || adminOpt.get().getRole() == null || !adminOpt.get().getRole().toString().equals("HOTEL_ADMIN")) {
                return reservationUtils.getResponseEntity("Hotel admin not found or role is invalid", HttpStatus.BAD_REQUEST);
            }
            List<hotel> existingHotelsForAdmin = hotelDao.findByAdminHotelierId(adminId);
            if (!existingHotelsForAdmin.isEmpty()) {
                return reservationUtils.getResponseEntity("This admin is already assigned to another hotel", HttpStatus.BAD_REQUEST);
            }

            hotel.setAdminHotelier(adminOpt.get());

            // Save to database
            hotelDao.save(hotel);
            return reservationUtils.getResponseEntity("Hotel added successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateHotelMap(Map<String, String> requestMap) {
        return requestMap.containsKey("nom") &&
                requestMap.containsKey("adresse") &&
                requestMap.containsKey("telephone") &&
                requestMap.containsKey("email") &&
                requestMap.containsKey("nombreEtoiles") &&
                requestMap.containsKey("image") &&
                requestMap.containsKey("adminHotelId");
    }


    @Override
    public ResponseEntity<String> updateHotel(Map<String, String> requestMap) {
        try {
            // Only GLOBAL_ADMIN can update hotels
            if (!jwtFilter.isGlobalAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Required field "id" is needed to find the hotel
            if (!requestMap.containsKey("id")) {
                return new ResponseEntity<>("{\"message\":\"Hotel ID is required.\"}", HttpStatus.BAD_REQUEST);

            }

            Long hotelId = Long.parseLong(requestMap.get("id"));
            Optional<hotel> optionalHotel = hotelDao.findById(hotelId);

            if (optionalHotel.isEmpty()) {
                return reservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            // Validate hotel fields
            if (!validateHotelMap(requestMap)) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            hotel existingHotel = optionalHotel.get();
            existingHotel.setNom(requestMap.get("nom"));
            existingHotel.setAdresse(requestMap.get("adresse"));
            existingHotel.setTelephone(requestMap.get("telephone"));
            existingHotel.setEmail(requestMap.get("email"));
            existingHotel.setImage(requestMap.get("image"));

            try {
                existingHotel.setNombreEtoiles(Integer.parseInt(requestMap.get("nombreEtoiles")));
            } catch (NumberFormatException ex) {
                return reservationUtils.getResponseEntity("Invalid number of stars", HttpStatus.BAD_REQUEST);
            }

            // Check hotel admin
            Long adminId = Long.parseLong(requestMap.get("adminHotelId"));
            Optional<user> adminOpt = userDao.findById(adminId);

            if (adminOpt.isEmpty() || adminOpt.get().getRole() == null || !adminOpt.get().getRole().toString().equals("HOTEL_ADMIN")) {
                return reservationUtils.getResponseEntity("Hotel admin not found or role is invalid", HttpStatus.BAD_REQUEST);
            }

            List<hotel> existingHotelsForAdmin = hotelDao.findByAdminHotelierId(adminId);
            boolean isAssignedToAnotherHotel = existingHotelsForAdmin.stream()
                    .anyMatch(h -> !h.getId().equals(hotelId));

            if (isAssignedToAnotherHotel) {
                return reservationUtils.getResponseEntity("This admin is already assigned to another hotel", HttpStatus.BAD_REQUEST);
            }
            existingHotel.setAdminHotelier(adminOpt.get());

            hotelDao.save(existingHotel);
            return reservationUtils.getResponseEntity("Hotel updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> deleteHotel(Long id) {
        try {
            // Vérifier que l'utilisateur est un GLOBAL_ADMIN
            if (!jwtFilter.isGlobalAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Vérifier que l'hôtel existe
            Optional<hotel> hotelOpt = hotelDao.findById(id);
            if (hotelOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            // Supprimer l'hôtel
            hotelDao.deleteById(id);
            return reservationUtils.getResponseEntity("Hotel deleted successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> getAllHotels() {
        try {
            // Vérifie si l'utilisateur est un GLOBAL_ADMIN
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            // Récupère tous les hôtels via la méthode personnalisée du repository
            List<HotelWrapper> hotelList = hotelDao.getAllHotels();

            // Vérifie si la liste est vide
            if (hotelList == null || hotelList.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            // Retourne la liste avec un statut OK
            return new ResponseEntity<>(hotelList, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @Override
    public ResponseEntity<HotelWrapper> getHotelById(Long id) {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            Optional<HotelWrapper> optionalHotel = hotelDao.getHotelById(id);

            if (optionalHotel.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(optionalHotel.get(), HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<List<HotelWrapper>> getHotelsByAdminId(Long adminId) {
        try {
            // Vérifier que l'utilisateur est un GLOBAL_ADMIN
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            // Récupérer les hôtels assignés à cet admin
            List<HotelWrapper> hotelList = hotelDao.getHotelsByAdminId(adminId);

            // Vérifie si la liste est vide
            if (hotelList == null || hotelList.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            // Retourne la liste
            return new ResponseEntity<>(hotelList, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> searchHotels(String nom, String adresse, Integer etoiles) {
        try {
            if (jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            List<HotelWrapper> hotels = hotelDao.searchHotels(nom, adresse, etoiles);

            if (hotels == null || hotels.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(hotels, HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
