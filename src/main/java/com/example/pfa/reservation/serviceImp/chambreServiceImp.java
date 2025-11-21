package com.example.pfa.reservation.serviceImp;

import com.example.pfa.reservation.JWT.JwtFilter;
import com.example.pfa.reservation.Repository.chambreDAO;
import com.example.pfa.reservation.Repository.hotelDAO;
import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.model.chambre;
import com.example.pfa.reservation.model.hotel;
import com.example.pfa.reservation.service.chambreService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.ChambreWrapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class chambreServiceImp implements chambreService {

    @Autowired
    private chambreDAO chambreDao;

    @Autowired
    private hotelDAO hotelDao;

    @Autowired
    private JwtFilter jwtFilter;

    private boolean validateChambreMap(Map<String, String> requestMap) {
        return requestMap.containsKey("numero")&&
                requestMap.containsKey("typeChambre") &&
                requestMap.containsKey("prix") &&
                requestMap.containsKey("disponibilite") &&
                requestMap.containsKey("hotelId");
    }

    @Override
    public ResponseEntity<String> addChambre(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!validateChambreMap(requestMap)) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            chambre chambre = new chambre();
            chambre.setNumero(requestMap.get("numero"));
            chambre.setTypeChambre(requestMap.get("typeChambre"));
            chambre.setDisponibilite(Boolean.parseBoolean(requestMap.get("disponibilite")));

            try {
                chambre.setPrix(Double.parseDouble(requestMap.get("prix")));
            } catch (NumberFormatException ex) {
                return reservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get("hotelId"));
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            hotel hotel = hotelOpt.get();

            // Récupérer le username ou email du user connecté
            String currentLogin = jwtFilter.getCurreentUser();

            // Vérifier si l'utilisateur est bien l'admin de l'hôtel
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return reservationUtils.getResponseEntity("Unauthorized to add a room to this hotel", HttpStatus.UNAUTHORIZED);
            }

            chambre.setHotel(hotel);

            chambreDao.save(chambre);
            return reservationUtils.getResponseEntity("Chambre added successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> updateChambre(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!requestMap.containsKey("id")) {
                return reservationUtils.getResponseEntity("Chambre ID is required", HttpStatus.BAD_REQUEST);
            }

            Long chambreId = Long.parseLong(requestMap.get("id"));
            Optional<chambre> chambreOpt = chambreDao.findById(chambreId);

            if (chambreOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Chambre not found", HttpStatus.NOT_FOUND);
            }

            chambre existingChambre = chambreOpt.get();
            hotel associatedHotel = existingChambre.getHotel();

            // Vérifier que l'utilisateur connecté est bien l'admin de cet hôtel
            String currentLogin = jwtFilter.getCurreentUser();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return reservationUtils.getResponseEntity("Unauthorized to update a room of this hotel", HttpStatus.UNAUTHORIZED);
            }

            if (!validateChambreMap(requestMap)) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            existingChambre.setNumero(requestMap.get("numero"));
            existingChambre.setTypeChambre(requestMap.get("typeChambre"));
            existingChambre.setDisponibilite(Boolean.parseBoolean(requestMap.get("disponibilite")));

            try {
                existingChambre.setPrix(Double.parseDouble(requestMap.get("prix")));
            } catch (NumberFormatException ex) {
                return reservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get("hotelId"));
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }
            existingChambre.setHotel(hotelOpt.get());

            chambreDao.save(existingChambre);
            return reservationUtils.getResponseEntity("Chambre updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteChambre(Long id) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            Optional<chambre> chambreOpt = chambreDao.findById(id);
            if (chambreOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Chambre not found", HttpStatus.NOT_FOUND);
            }

            chambre existingChambre = chambreOpt.get();
            hotel associatedHotel = existingChambre.getHotel();

            // Vérifier que l'utilisateur connecté est bien l'admin de cet hôtel
            String currentLogin = jwtFilter.getCurreentUser();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return reservationUtils.getResponseEntity("Unauthorized to delete a room of this hotel", HttpStatus.UNAUTHORIZED);
            }

            chambreDao.deleteById(id);
            return reservationUtils.getResponseEntity("Chambre deleted successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<List<ChambreWrapper>> getAllChambres() {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            List<ChambreWrapper> chambres = chambreDao.getAllChambres();
            if (chambres == null || chambres.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(chambres, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ChambreWrapper> getChambreById(Long id) {
        try {
            // Vérifier si l'utilisateur connecté est un admin hôtelier
            if (!jwtFilter.isHotelAdmin()) {
                // Retourner une réponse "Unauthorized" avec code 401 et corps null
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            // Récupérer la chambre par ID
            Optional<ChambreWrapper> chambreOpt = chambreDao.getChambreById(id);
            if (chambreOpt.isEmpty()) {
                // Retourner une réponse "Not Found" avec code 404 et corps null
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            ChambreWrapper chambre = chambreOpt.get();

            // Vérifier si l'admin hôtelier connecté a accès à cette chambre
            Long hotelId = chambre.getHotelId(); // Récupérer l'ID de l'hôtel de la chambre
            String currentLogin = jwtFilter.getCurreentUser(); // Récupérer le login de l'utilisateur connecté

            // Trouver l'hôtel associé à la chambre et vérifier que l'admin connecté est l'admin de cet hôtel
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                // Retourner une réponse "Not Found" si l'hôtel n'existe pas, avec code 404 et corps null
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            hotel hotel = hotelOpt.get();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            // Vérifier que l'utilisateur connecté est bien l'admin de l'hôtel
            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                // Retourner une réponse "Unauthorized" si l'utilisateur n'est pas l'admin de l'hôtel
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            // Si l'utilisateur est autorisé, renvoyer la chambre
            return new ResponseEntity<>(chambre, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            // En cas d'erreur interne, renvoyer une réponse avec code 500 et corps null
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<List<ChambreWrapper>> getChambresByHotelId(Long hotelId) {
        try {
            // Vérifier si l'utilisateur connecté est un admin hôtelier
            if (!jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            // Trouver l'hôtel par son ID
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                // Hôtel non trouvé
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            hotel hotel = hotelOpt.get();

            // Récupérer l'utilisateur connecté
            String currentLogin = jwtFilter.getCurreentUser();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            // Vérifier que l'utilisateur est bien l'admin de cet hôtel
            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            // Récupérer les chambres liées à l'hôtel
            List<ChambreWrapper> chambres = chambreDao.getChambresByHotelId(hotelId);
            if (chambres == null || chambres.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
            }

            // Retourner les chambres si tout est bon
            return new ResponseEntity<>(chambres, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            // En cas d'erreur interne
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<List<ChambreWrapper>> searchChambres(String typeChambre, Double prixMin, Double prixMax, Boolean disponibilite) {
        try {
            if (jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
            List<ChambreWrapper> chambres = chambreDao.searchChambres(typeChambre, prixMin, prixMax, disponibilite);
            if (chambres == null || chambres.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(chambres, HttpStatus.OK);

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

            // Vérifier que l'hôtel existe
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            // Si c'est un admin hôtelier, vérifier qu'il est bien l'admin de cet hôtel
            if (jwtFilter.isHotelAdmin()) {
                hotel hotel = hotelOpt.get();
                String currentLogin = jwtFilter.getCurreentUser();
                String adminEmail = hotel.getAdminHotelier().getEmail();
                String adminUsername = hotel.getAdminHotelier().getUserName();

                if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                    return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
                }
            }

            // Récupérer les chambres disponibles
            List<ChambreWrapper> chambresDisponibles = chambreDao.getAvailableChambresByHotelId(hotelId);
            if (chambresDisponibles == null || chambresDisponibles.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(chambresDisponibles, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
