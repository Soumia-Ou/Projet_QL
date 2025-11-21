package com.example.pfa.reservation.serviceImp;

import com.example.pfa.reservation.JWT.JwtFilter;
import com.example.pfa.reservation.Repository.*;
import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.model.*;
import com.example.pfa.reservation.service.reservationService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.ChambreWrapper;
import com.example.pfa.reservation.wrapper.ReservationWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j //logging propre et efficace
@Service
public class reservationServiceImp implements reservationService {
    @Autowired
    private chambreDAO chambreDao;

    @Autowired
    private hotelDAO hotelDao;

    @Autowired
    private userDAO userDao;

    @Autowired
    private serviceDAO serviceDao;

    @Autowired
    private reservationDAO reservationDao;

    @Autowired
    private JwtFilter jwtFilter;

    @Override
    @Transactional
    public ResponseEntity<String> addReservation(Map<String, String> requestMap) {
        try {
            // Vérification si l'utilisateur est un client
            if (!jwtFilter.isClient()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Vérification des champs requis
            if (!requestMap.containsKey("chambreId") || !requestMap.containsKey("dateDebut") || !requestMap.containsKey("dateFin")) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            Long chambreId = Long.parseLong(requestMap.get("chambreId"));
            LocalDate dateDebut = LocalDate.parse(requestMap.get("dateDebut"));
            LocalDate dateFin = LocalDate.parse(requestMap.get("dateFin"));

            Optional<chambre> chambreOpt = chambreDao.findById(chambreId);
            if (chambreOpt.isEmpty() || !chambreOpt.get().getDisponibilite()) {
                return reservationUtils.getResponseEntity("Room is not available", HttpStatus.BAD_REQUEST);
            }

            reservation newReservation = new reservation();
            newReservation.setDateDebut(dateDebut);
            newReservation.setDateFin(dateFin);
            newReservation.setDateReservation(LocalDateTime.now());
            newReservation.setStatut(ReservationStatus.PENDING);

            String currentLogin = jwtFilter.getCurreentUser();
            user client = userDao.findByEmail(currentLogin);
            if (client == null) {
                return reservationUtils.getResponseEntity("Client not found", HttpStatus.NOT_FOUND);
            }
            newReservation.setClient(client);
            newReservation.setChambre(chambreOpt.get());

            hotel associatedHotel = chambreOpt.get().getHotel();
            if (associatedHotel == null) {
                return reservationUtils.getResponseEntity("Associated hotel not found for the room", HttpStatus.BAD_REQUEST);
            }
            newReservation.setHotel(associatedHotel); // 🔧 Fix du problème @NotNull


            Double totalAmount = chambreOpt.get().getPrix(); // Prix de la chambre

            // Traitement des services optionnels
            if (requestMap.containsKey("services") && !requestMap.get("services").isBlank()) {
                String[] serviceIds = requestMap.get("services").split(",");
                List<service> selectedServices = new ArrayList<>();

                for (String idStr : serviceIds) {
                    try {
                        Long serviceId = Long.parseLong(idStr.trim());
                        Optional<service> optService = serviceDao.findById(serviceId);
                        if (optService.isPresent()) {
                            selectedServices.add(optService.get());
                            totalAmount += optService.get().getPrix(); // Ajouter le prix du service
                        } else {
                            return reservationUtils.getResponseEntity("Service not found: ID = " + serviceId, HttpStatus.BAD_REQUEST);
                        }
                    } catch (NumberFormatException e) {
                        return reservationUtils.getResponseEntity("Invalid service ID format: " + idStr, HttpStatus.BAD_REQUEST);
                    }
                }

                newReservation.setServices(selectedServices); // ⚠ Assure-toi que ce setter existe
            }

            newReservation.setMontantTotal(totalAmount);

            reservationDao.save(newReservation);
            return reservationUtils.getResponseEntity("Reservation added successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    @Override
    @Transactional
    public ResponseEntity<String> updateReservation(Map<String, String> requestMap) {
        try {
//            // Vérification que l'utilisateur est un client
//            if (!jwtFilter.isClient()) {
//                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
//            }

            // Vérification des champs requis
            if (!requestMap.containsKey("id")) {
                return reservationUtils.getResponseEntity("Reservation ID is required", HttpStatus.BAD_REQUEST);
            }

            // Récupération de l'utilisateur courant
            String currentUserEmail = jwtFilter.getCurreentUser();
            user currentUser = userDao.findByEmail(currentUserEmail);

            if (currentUser == null) {
                return reservationUtils.getResponseEntity("User not found", HttpStatus.NOT_FOUND);
            }

            Long reservationId = Long.parseLong(requestMap.get("id"));
            Optional<reservation> existingReservationOpt = reservationDao.findById(reservationId);

            if (existingReservationOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Reservation not found", HttpStatus.NOT_FOUND);
            }

            reservation existingReservation = existingReservationOpt.get();

            // Vérification que le client est bien le propriétaire de la réservation
            if (!existingReservation.getClient().getId().equals(currentUser.getId())) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Vérifier que la réservation est encore modifiable (statut PENDING)
            if (existingReservation.getStatut() != ReservationStatus.PENDING) {
                return reservationUtils.getResponseEntity("Only PENDING reservations can be modified", HttpStatus.BAD_REQUEST);
            }

            // Mise à jour de la chambre si spécifiée
            if (requestMap.containsKey("chambreId")) {
                Long newChambreId = Long.parseLong(requestMap.get("chambreId"));
                Optional<chambre> newChambreOpt = chambreDao.findById(newChambreId);

                if (newChambreOpt.isEmpty() || !newChambreOpt.get().getDisponibilite()) {
                    return reservationUtils.getResponseEntity("New room is not available", HttpStatus.BAD_REQUEST);
                }

                chambre newChambre = newChambreOpt.get();
                existingReservation.setChambre(newChambre);
                existingReservation.setHotel(newChambre.getHotel()); // Mise à jour de l'hôtel associé

                // Réinitialisation du montant total avec le nouveau prix de la chambre
                Double totalAmount = newChambre.getPrix();

                // Conservation des services existants si non modifiés
                if (existingReservation.getServices() != null && !requestMap.containsKey("services")) {
                    totalAmount += existingReservation.getServices().stream()
                            .mapToDouble(service::getPrix)
                            .sum();
                }
                existingReservation.setMontantTotal(totalAmount);
            }

            // Mise à jour des dates si spécifiées
            if (requestMap.containsKey("dateDebut")) {
                LocalDate newDateDebut = LocalDate.parse(requestMap.get("dateDebut"));
                existingReservation.setDateDebut(newDateDebut);

                // Validation cohérence des dates
                if (requestMap.containsKey("dateFin")) {
                    LocalDate newDateFin = LocalDate.parse(requestMap.get("dateFin"));
                    if (newDateDebut.isAfter(newDateFin)) {
                        return reservationUtils.getResponseEntity("Start date must be before end date", HttpStatus.BAD_REQUEST);
                    }
                    existingReservation.setDateFin(newDateFin);
                } else if (newDateDebut.isAfter(existingReservation.getDateFin())) {
                    return reservationUtils.getResponseEntity("New start date must be before existing end date", HttpStatus.BAD_REQUEST);
                }
            } else if (requestMap.containsKey("dateFin")) {
                LocalDate newDateFin = LocalDate.parse(requestMap.get("dateFin"));
                if (existingReservation.getDateDebut().isAfter(newDateFin)) {
                    return reservationUtils.getResponseEntity("Existing start date must be before new end date", HttpStatus.BAD_REQUEST);
                }
                existingReservation.setDateFin(newDateFin);
            }

            // Mise à jour des services si spécifiés
            if (requestMap.containsKey("services")) {
                Double totalAmount = existingReservation.getChambre().getPrix();
                List<service> selectedServices = new ArrayList<>();

                if (!requestMap.get("services").isBlank()) {
                    String[] serviceIds = requestMap.get("services").split(",");

                    for (String idStr : serviceIds) {
                        try {
                            Long serviceId = Long.parseLong(idStr.trim());
                            Optional<service> optService = serviceDao.findById(serviceId);
                            if (optService.isPresent()) {
                                selectedServices.add(optService.get());
                                totalAmount += optService.get().getPrix();
                            } else {
                                return reservationUtils.getResponseEntity("Service not found: ID = " + serviceId, HttpStatus.BAD_REQUEST);
                            }
                        } catch (NumberFormatException e) {
                            return reservationUtils.getResponseEntity("Invalid service ID format: " + idStr, HttpStatus.BAD_REQUEST);
                        }
                    }
                }

                existingReservation.setServices(selectedServices);
                existingReservation.setMontantTotal(totalAmount);
            }

            // On force le statut à PENDING même si quelqu'un essaye de le modifier
            existingReservation.setStatut(ReservationStatus.PENDING);

            // Mise à jour de la date de modification
            existingReservation.setDateReservation(LocalDateTime.now());

            // Sauvegarde des modifications
            reservationDao.save(existingReservation);
            return reservationUtils.getResponseEntity("Reservation updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    @Transactional
    public ResponseEntity<String> deleteReservation(Long id) {
        try {
            Optional<reservation> reservationOpt = reservationDao.findById(id);
            if (reservationOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Reservation not found", HttpStatus.NOT_FOUND);
            }

            reservation res = reservationOpt.get();

            String currentUserEmail = jwtFilter.getCurreentUser();
            user currentUser = userDao.findByEmail(currentUserEmail);

            if (currentUser == null) {
                return reservationUtils.getResponseEntity("User not found", HttpStatus.NOT_FOUND);
            }

            boolean isClient = jwtFilter.isClient();
            boolean isHotelAdmin = jwtFilter.isHotelAdmin();

            // Client : peut supprimer uniquement sa propre réservation PENDING
            if (isClient) {
                if (!res.getClient().getId().equals(currentUser.getId())) {
                    return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
                }
                if (res.getStatut() != ReservationStatus.PENDING) {
                    return reservationUtils.getResponseEntity("Only PENDING reservations can be deleted by clients", HttpStatus.BAD_REQUEST);
                }
            }

            // Admin Hôtelier : peut supprimer les réservations des chambres de son hôtel
            else if (isHotelAdmin) {
                hotel hotelOfReservation = res.getHotel();
                if (hotelOfReservation == null || !hotelOfReservation.getAdminHotelier().getId().equals(currentUser.getId())) {
                    return reservationUtils.getResponseEntity("You are not authorized to delete this reservation", HttpStatus.UNAUTHORIZED);
                }
            }

            // Sinon, l'utilisateur n'a pas les droits requis
            else {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Suppression
            reservationDao.deleteById(id);
            return reservationUtils.getResponseEntity("Reservation deleted successfully", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> getReservationsByClientId(Long clientId) {
        try {
            // 1. Authenticate the request by extracting the current user's email from JWT
            String currentUserEmail = jwtFilter.getCurreentUser();
            if (currentUserEmail == null) {
                // User not authenticated
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            // 2. Retrieve the current user entity by email
            user currentUser = userDao.findByEmail(currentUserEmail);
            if (currentUser == null) {
                // User not found in database
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 3. Authorization: Ensure the current user is a client AND is requesting only their own reservations
            if (!jwtFilter.isClient() || !currentUser.getId().equals(clientId)) {
                // Forbidden access if user is not a client or tries to access someone else's reservations
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // 4. Fetch reservations for the client
            List<ReservationWrapper> reservations = reservationDao.findReservationsByClientId(clientId);

            // 5. Return the list of reservations with HTTP 200 OK
            return new ResponseEntity<>(reservations, HttpStatus.OK);

        } catch (Exception ex) {
            // Log the exception and return 500 error
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    @Transactional
    public ResponseEntity<List<ReservationWrapper>> getReservationsByHotelId(Long hotelId) {
        try {
            String currentUserEmail = jwtFilter.getCurreentUser();
            if (currentUserEmail == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            user currentUser = userDao.findByEmail(currentUserEmail);
            if (currentUser == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            if (!jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            hotel hotel = hotelOpt.get();

            if (hotel.getAdminHotelier() == null || !hotel.getAdminHotelier().getId().equals(currentUser.getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // Récupération des entités reservations
            List<reservation> reservationEntities = reservationDao.findReservationsByHotelId(hotelId);

            List<ReservationWrapper> reservations = reservationEntities.stream()
                    .map(ReservationWrapper::fromEntity)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(reservations, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> searchReservations(Long clientId, Long hotelId,
                                                                       LocalDate dateDebut, LocalDate dateFin,
                                                                       String statut) {
        try {
            // Vérification des autorisations (garder votre code existant)
            if (!jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            String username = jwtFilter.getCurreentUser();
            user admin = userDao.findByEmailOrUsername(username, username);
            if (admin == null || !admin.getRole().toString().equals("HOTEL_ADMIN")) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            // Récupérer les hôtels de l'admin
            List<hotel> adminHotels = hotelDao.findByAdminHotelierId(admin.getId());
            if (adminHotels.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            List<Long> adminHotelIds = adminHotels.stream()
                    .map(hotel::getId)
                    .toList();

            // Vérification que l'hôtel demandé appartient à l'admin
            if (hotelId != null && !adminHotelIds.contains(hotelId)) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            // Utilisation de la méthode DTO avec filtrage strict du statut
            List<ReservationWrapper> result = new ArrayList<>();

            if (hotelId != null) {
                // Si statut est fourni, filtrer strictement
                if (statut != null) {
                    result = reservationDao.searchReservations(
                            clientId, hotelId, dateDebut, dateFin, statut.toUpperCase());
                } else {
                    result = reservationDao.searchReservations(
                            clientId, hotelId, dateDebut, dateFin, null);
                }
            } else {
                // Pour tous les hôtels de l'admin
                for (Long hId : adminHotelIds) {
                    if (statut != null) {
                        result.addAll(reservationDao.searchReservations(
                                clientId, hId, dateDebut, dateFin, statut.toUpperCase()));
                    } else {
                        result.addAll(reservationDao.searchReservations(
                                clientId, hId, dateDebut, dateFin, null));
                    }
                }
            }

            // Filtrer les résultats vides
            result = result.stream()
                    .filter(r -> statut == null || r.getStatut().toString().equals(statut))
                    .collect(Collectors.toList());

            return result.isEmpty()
                    ? new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT)
                    : ResponseEntity.ok(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReservationWrapper>> getActiveReservations() {
        try {
            // Vérification de l'identité de l'utilisateur connecté
            String currentUserEmail = jwtFilter.getCurreentUser();
            if (currentUserEmail == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            user currentUser = userDao.findByEmail(currentUserEmail);
            if (currentUser == null || !jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            // Récupérer les hôtels gérés par cet admin
            List<hotel> hotels = hotelDao.findByAdminHotelierId(currentUser.getId());
            if (hotels.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            List<Long> hotelIds = hotels.stream()
                    .map(hotel::getId)
                    .toList();

            // Obtenir toutes les réservations actives
            List<reservation> activeReservations = reservationDao.findActiveReservations(ReservationStatus.CONFIRMED);

            // Filtrer les réservations uniquement pour les hôtels de l'admin
            List<ReservationWrapper> result = activeReservations.stream()
                    .filter(r -> r.getHotel() != null && hotelIds.contains(r.getHotel().getId()))
                    .map(ReservationWrapper::fromEntity)
                    .collect(Collectors.toList());

            return result.isEmpty()
                    ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                    : new ResponseEntity<>(result, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Double> getTotalAmountByClientId(Long clientId) {
        try {
            String currentUserName = jwtFilter.getCurreentUser();
            if (currentUserName == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            user currentUser = userDao.findByEmailOrUsername(currentUserName, currentUserName);
            if (currentUser == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            boolean isClient = currentUser.getRole().toString().equals("CLIENT")
                    && currentUser.getId().equals(clientId);
            boolean isHotelAdmin = currentUser.getRole().toString().equals("HOTEL_ADMIN");

            if (!isClient && !isHotelAdmin) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            List<ReservationWrapper> reservations;

            if (isHotelAdmin) {
                List<Long> adminHotelIds = hotelDao.findByAdminHotelierId(currentUser.getId())
                        .stream()
                        .map(hotel::getId)
                        .toList();

                if (adminHotelIds.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }

                reservations = reservationDao.findReservationsByClientId(clientId)
                        .stream()
                        .filter(r -> r.getHotelId() != null && adminHotelIds.contains(r.getHotelId()))
                        .collect(Collectors.toList());
            } else {
                reservations = reservationDao.findReservationsByClientId(clientId);
            }

            if (reservations.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            double total = reservations.stream()
                    .filter(r -> r.getMontantTotal() != null)
                    .mapToDouble(ReservationWrapper::getMontantTotal)
                    .sum();

            return ResponseEntity.ok(total);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<Double> calculerPrixTotalReservation(Long reservationId) {
        return null;
    }

    @Override
    @Transactional
    public ResponseEntity<String> confirmerReservation(Long id) {
        try {
            // 1. Authentification
            String currentUserName = jwtFilter.getCurreentUser();
            if (currentUserName == null) {
                return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            user currentUser = userDao.findByEmailOrUsername(currentUserName, currentUserName);
            if (currentUser == null) {
                return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
            }

            // 2. Recherche réservation
            Optional<reservation> optionalReservation = reservationDao.findById(id);
            if (optionalReservation.isEmpty()) {
                return new ResponseEntity<>("Reservation not found", HttpStatus.NOT_FOUND);
            }

            reservation res = optionalReservation.get();

            // 3. Vérifier statut PENDING
            if (res.getStatut() != ReservationStatus.PENDING) {
                return new ResponseEntity<>("Reservation is not in PENDING status", HttpStatus.BAD_REQUEST);
            }

            // 4. Autorisation : client ou admin hotelier
            boolean isClient = currentUser.getRole().toString().equals("CLIENT") &&
                    res.getClient() != null &&
                    res.getClient().getId().equals(currentUser.getId());

            boolean isAdminHotelier = currentUser.getRole().toString().equals("HOTEL_ADMIN") &&
                    res.getHotel() != null &&
                    res.getHotel().getAdminHotelier() != null &&
                    res.getHotel().getAdminHotelier().getId().equals(currentUser.getId());

            // 5. Vérifier si confirmation automatique (dateDebut dans 1 jour ou moins)
            boolean isNearStartDate = res.getDateDebut() != null &&
                    !LocalDate.now().isAfter(res.getDateDebut()) && // aujourd'hui ou avant la date de début
                    ChronoUnit.DAYS.between(LocalDate.now(), res.getDateDebut()) <= 1;

            if (!isClient && !isAdminHotelier && !isNearStartDate) {
                return new ResponseEntity<>("You are not authorized to confirm this reservation", HttpStatus.UNAUTHORIZED);
            }

            // 6. Changer le statut et sauvegarder
            res.setStatut(ReservationStatus.CONFIRMED);
            reservationDao.save(res);

            return new ResponseEntity<>("Reservation confirmed successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    @Transactional
    public ResponseEntity<String> annulerReservation(Long id) {
        try {
            // 1. Authentification
            String currentUserName = jwtFilter.getCurreentUser();
            if (currentUserName == null) {
                return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            user currentUser = userDao.findByEmailOrUsername(currentUserName, currentUserName);
            if (currentUser == null) {
                return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
            }

            // 2. Recherche réservation
            Optional<reservation> optionalReservation = reservationDao.findById(id);
            if (optionalReservation.isEmpty()) {
                return new ResponseEntity<>("Reservation not found", HttpStatus.NOT_FOUND);
            }

            reservation res = optionalReservation.get();

            // 3. Vérifier que la réservation est en statut PENDING uniquement
            if (res.getStatut() != ReservationStatus.PENDING) {
                return new ResponseEntity<>("Only PENDING reservations can be cancelled", HttpStatus.BAD_REQUEST);
            }

            // 4. Autorisation : client ou admin hotelier
            boolean isClient = currentUser.getRole().toString().equals("CLIENT") &&
                    res.getClient() != null &&
                    res.getClient().getId().equals(currentUser.getId());

            boolean isAdminHotelier = currentUser.getRole().toString().equals("HOTEL_ADMIN") &&
                    res.getHotel() != null &&
                    res.getHotel().getAdminHotelier() != null &&
                    res.getHotel().getAdminHotelier().getId().equals(currentUser.getId());

            if (!isClient && !isAdminHotelier) {
                return new ResponseEntity<>("You are not authorized to cancel this reservation", HttpStatus.UNAUTHORIZED);
            }

            // 5. Changer le statut et sauvegarder
            res.setStatut(ReservationStatus.CANCELLED);
            reservationDao.save(res);

            return new ResponseEntity<>("Reservation cancelled successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




}
