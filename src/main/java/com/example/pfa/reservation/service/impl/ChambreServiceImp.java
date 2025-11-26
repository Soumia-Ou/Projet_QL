package com.example.pfa.reservation.service.impl;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.model.Chambre;
import com.example.pfa.reservation.model.Hotel;
import com.example.pfa.reservation.repository.ChambreDAO;
import com.example.pfa.reservation.repository.HotelDAO;
import com.example.pfa.reservation.service.ChambreService;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.ChambreWrapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ChambreServiceImp implements ChambreService {

    private final ChambreDAO chambreDao;
    private final HotelDAO hotelDao;
    private final JwtFilter jwtFilter;

    public ChambreServiceImp(ChambreDAO chambreDao, HotelDAO hotelDao, JwtFilter jwtFilter) {
        this.chambreDao = chambreDao;
        this.hotelDao = hotelDao;
        this.jwtFilter = jwtFilter;
    }

    private static final String NUMERO = "numero";
    private static final String TYPE_CHAMBRE = "typeChambre";
    private static final String DISPONIBILITE = "disponibilite";
    private static final String HOTEL_ID = "hotelId";
    private static final String PRIX = "prix";

    private boolean validateChambreMap(Map<String, String> requestMap) {
        return requestMap.containsKey(NUMERO) &&
                requestMap.containsKey(TYPE_CHAMBRE) &&
                requestMap.containsKey(PRIX) &&
                requestMap.containsKey(DISPONIBILITE) &&
                requestMap.containsKey(HOTEL_ID);
    }

    private ResponseEntity<String> setPrixSafely(Chambre chambre, String prixStr) {
        try {
            chambre.setPrix(Double.parseDouble(prixStr));
            return null;
        } catch (NumberFormatException e) {
            return ReservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<String> addChambre(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!validateChambreMap(requestMap)) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            Chambre chambre = new Chambre();
            chambre.setNumero(requestMap.get(NUMERO));
            chambre.setTypeChambre(requestMap.get(TYPE_CHAMBRE));
            chambre.setDisponibilite(Boolean.parseBoolean(requestMap.get(DISPONIBILITE)));

            ResponseEntity<String> prixError = setPrixSafely(chambre, requestMap.get(PRIX));
            if (prixError != null) return prixError;

            Long hotelId = Long.parseLong(requestMap.get(HOTEL_ID));
            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            Hotel hotel = hotelOpt.get();

            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return ReservationUtils.getResponseEntity("Unauthorized to add a room to this hotel", HttpStatus.UNAUTHORIZED);
            }

            chambre.setHotel(hotel);
            chambreDao.save(chambre);
            return ReservationUtils.getResponseEntity("Chambre added successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> updateChambre(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!requestMap.containsKey("id")) {
                return ReservationUtils.getResponseEntity("Chambre ID is required", HttpStatus.BAD_REQUEST);
            }

            Long chambreId = Long.parseLong(requestMap.get("id"));
            Optional<Chambre> chambreOpt = chambreDao.findById(chambreId);
            if (chambreOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Chambre not found", HttpStatus.NOT_FOUND);
            }

            Chambre existingChambre = chambreOpt.get();
            Hotel associatedHotel = existingChambre.getHotel();

            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return ReservationUtils.getResponseEntity("Unauthorized to update a room of this hotel", HttpStatus.UNAUTHORIZED);
            }

            if (!validateChambreMap(requestMap)) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            existingChambre.setNumero(requestMap.get(NUMERO));
            existingChambre.setTypeChambre(requestMap.get(TYPE_CHAMBRE));
            existingChambre.setDisponibilite(Boolean.parseBoolean(requestMap.get(DISPONIBILITE)));

            ResponseEntity<String> prixError = setPrixSafely(existingChambre, requestMap.get(PRIX));
            if (prixError != null) return prixError;

            Long hotelId = Long.parseLong(requestMap.get(HOTEL_ID));
            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }
            existingChambre.setHotel(hotelOpt.get());

            chambreDao.save(existingChambre);
            return ReservationUtils.getResponseEntity("Chambre updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteChambre(Long id) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            Optional<Chambre> chambreOpt = chambreDao.findById(id);
            if (chambreOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Chambre not found", HttpStatus.NOT_FOUND);
            }

            Chambre existingChambre = chambreOpt.get();
            Hotel associatedHotel = existingChambre.getHotel();

            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return ReservationUtils.getResponseEntity("Unauthorized to delete a room of this hotel", HttpStatus.UNAUTHORIZED);
            }

            chambreDao.deleteById(id);
            return ReservationUtils.getResponseEntity("Chambre deleted successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> getAllChambres() {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            List<ChambreWrapper> chambres = chambreDao.getAllChambres();
            return (chambres == null || chambres.isEmpty())
                    ? new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT)
                    : new ResponseEntity<>(chambres, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ChambreWrapper> getChambreById(Long id) {
        try {
            if (!jwtFilter.isHotelAdmin()) return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);

            Optional<ChambreWrapper> chambreOpt = chambreDao.getChambreById(id);
            if (chambreOpt.isEmpty()) return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

            ChambreWrapper chambre = chambreOpt.get();
            Optional<Hotel> hotelOpt = hotelDao.findById(chambre.getHotelId());
            if (hotelOpt.isEmpty()) return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

            Hotel hotel = hotelOpt.get();
            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            return new ResponseEntity<>(chambre, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> getChambresByHotelId(Long hotelId) {
        try {
            if (!jwtFilter.isHotelAdmin()) return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);

            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

            Hotel hotel = hotelOpt.get();
            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            List<ChambreWrapper> chambres = chambreDao.getChambresByHotelId(hotelId);
            return (chambres == null || chambres.isEmpty())
                    ? new ResponseEntity<>(null, HttpStatus.NO_CONTENT)
                    : new ResponseEntity<>(chambres, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> searchChambres(String typeChambre, Double prixMin, Double prixMax, Boolean disponibilite) {
        try {
            if (!jwtFilter.isGlobalAdmin()) return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);

            List<ChambreWrapper> chambres = chambreDao.searchChambres(typeChambre, prixMin, prixMax, disponibilite);
            return (chambres == null || chambres.isEmpty())
                    ? new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT)
                    : new ResponseEntity<>(chambres, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> getAvailableChambresByHotelId(Long hotelId) {
        try {
            if (!(jwtFilter.isGlobalAdmin() || jwtFilter.isHotelAdmin() || jwtFilter.isClient())) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

            if (jwtFilter.isHotelAdmin()) {
                Hotel hotel = hotelOpt.get();
                String currentLogin = jwtFilter.getCurrentUserRole();
                String adminEmail = hotel.getAdminHotelier().getEmail();
                String adminUsername = hotel.getAdminHotelier().getUserName();

                if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                    return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
                }
            }

            List<ChambreWrapper> chambresDisponibles = chambreDao.getAvailableChambresByHotelId(hotelId);
            return (chambresDisponibles == null || chambresDisponibles.isEmpty())
                    ? new ResponseEntity<>(null, HttpStatus.NO_CONTENT)
                    : new ResponseEntity<>(chambresDisponibles, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
