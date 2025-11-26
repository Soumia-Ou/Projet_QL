package com.example.pfa.reservation.service.impl;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.repository.*;
import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.model.*;
import com.example.pfa.reservation.service.ReservationService;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.ReservationWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReservationServiceImpl implements ReservationService {

    private final ChambreDAO chambreDao;
    private final UserDAO userDao;
    private final ServiceDAO serviceDao;
    private final ReservationDAO reservationDao;
    private final JwtFilter jwtFilter;

    public ReservationServiceImpl(ChambreDAO chambreDao, UserDAO userDao,
                                  ServiceDAO serviceDao, ReservationDAO reservationDao, JwtFilter jwtFilter) {
        this.chambreDao = chambreDao;
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.reservationDao = reservationDao;
        this.jwtFilter = jwtFilter;
    }

    // ------------------ UTILITAIRES ------------------

    private User getCurrentUser() {
        String email = jwtFilter.getCurrentUserRole();
        if (email == null) return null;
        return userDao.findByEmailOrUsername(email, email);
    }

    private boolean isClient(User user) {
        return user != null && "CLIENT".equals(user.getRole().toString());
    }

    private boolean isHotelAdmin(User user) {
        return user != null && "HOTEL_ADMIN".equals(user.getRole().toString());
    }

    // ------------------ CRUD RESERVATION ------------------

    @Override
    @Transactional
    public ResponseEntity<String> addReservation(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isClient()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!requestMap.containsKey("chambreId") || !requestMap.containsKey("dateDebut") || !requestMap.containsKey("dateFin")) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            Long chambreId = Long.parseLong(requestMap.get("chambreId"));
            LocalDate dateDebut = LocalDate.parse(requestMap.get("dateDebut"));
            LocalDate dateFin = LocalDate.parse(requestMap.get("dateFin"));

            Optional<Chambre> chambreOpt = chambreDao.findById(chambreId);
            if (chambreOpt.isEmpty() || !chambreOpt.get().getDisponibilite()) {
                return ReservationUtils.getResponseEntity("Room is not available", HttpStatus.BAD_REQUEST);
            }

            Reservation reservation = new Reservation();
            reservation.setDateDebut(dateDebut);
            reservation.setDateFin(dateFin);
            reservation.setDateReservation(LocalDateTime.now());
            reservation.setStatut(ReservationStatus.PENDING);

            User client = getCurrentUser();
            if (client == null) return ReservationUtils.getResponseEntity("Client not found", HttpStatus.NOT_FOUND);
            reservation.setClient(client);

            Chambre chambre = chambreOpt.get();
            reservation.setChambre(chambre);
            Hotel hotel = chambre.getHotel();
            if (hotel == null) return ReservationUtils.getResponseEntity("Associated hotel not found", HttpStatus.BAD_REQUEST);
            reservation.setHotel(hotel);

            double totalAmount = chambre.getPrix();

            // Services optionnels
            if (requestMap.containsKey("services") && !requestMap.get("services").isBlank()) {
                String[] serviceIds = requestMap.get("services").split(",");
                List<com.example.pfa.reservation.model.Service> selectedServices = new ArrayList<>();
                for (String idStr : serviceIds) {
                    Long serviceId = Long.parseLong(idStr.trim());
                    Optional<com.example.pfa.reservation.model.Service> serviceOpt = serviceDao.findById(serviceId);
                    if (serviceOpt.isPresent()) {
                        selectedServices.add(serviceOpt.get());
                        totalAmount += serviceOpt.get().getPrix();
                    } else {
                        return ReservationUtils.getResponseEntity("Service not found: ID = " + serviceId, HttpStatus.BAD_REQUEST);
                    }
                }
                reservation.setServices(selectedServices);
            }

            reservation.setMontantTotal(totalAmount);
            reservationDao.save(reservation);

            return ReservationUtils.getResponseEntity("Reservation added successfully", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> updateReservation(Map<String, String> requestMap) {
        try {
            if (!requestMap.containsKey("id")) {
                return ReservationUtils.getResponseEntity("Reservation ID is required", HttpStatus.BAD_REQUEST);
            }

            Long reservationId = Long.parseLong(requestMap.get("id"));
            Optional<Reservation> reservationOpt = reservationDao.findById(reservationId);
            if (reservationOpt.isEmpty()) return ReservationUtils.getResponseEntity("Reservation not found", HttpStatus.NOT_FOUND);

            Reservation reservation = reservationOpt.get();
            User currentUser = getCurrentUser();
            if (currentUser == null) return ReservationUtils.getResponseEntity("User not found", HttpStatus.NOT_FOUND);

            if (!reservation.getClient().getId().equals(currentUser.getId())) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (reservation.getStatut() != ReservationStatus.PENDING) {
                return ReservationUtils.getResponseEntity("Only PENDING reservations can be modified", HttpStatus.BAD_REQUEST);
            }

            // Update chambre
            if (requestMap.containsKey("chambreId")) {
                Long newChambreId = Long.parseLong(requestMap.get("chambreId"));
                Optional<Chambre> newChambreOpt = chambreDao.findById(newChambreId);
                if (newChambreOpt.isEmpty() || !newChambreOpt.get().getDisponibilite()) {
                    return ReservationUtils.getResponseEntity("New room is not available", HttpStatus.BAD_REQUEST);
                }
                Chambre newChambre = newChambreOpt.get();
                reservation.setChambre(newChambre);
                reservation.setHotel(newChambre.getHotel());
            }

            // Update dates
            if (requestMap.containsKey("dateDebut")) {
                LocalDate dateDebut = LocalDate.parse(requestMap.get("dateDebut"));
                reservation.setDateDebut(dateDebut);
            }
            if (requestMap.containsKey("dateFin")) {
                LocalDate dateFin = LocalDate.parse(requestMap.get("dateFin"));
                reservation.setDateFin(dateFin);
            }

            // Update services
            if (requestMap.containsKey("services")) {
                double totalAmount = reservation.getChambre().getPrix();
                List<com.example.pfa.reservation.model.Service> selectedServices = new ArrayList<>();
                String servicesStr = requestMap.get("services");
                if (!servicesStr.isBlank()) {
                    String[] serviceIds = servicesStr.split(",");
                    for (String idStr : serviceIds) {
                        Long serviceId = Long.parseLong(idStr.trim());
                        Optional<com.example.pfa.reservation.model.Service> serviceOpt = serviceDao.findById(serviceId);
                        if (serviceOpt.isPresent()) {
                            selectedServices.add(serviceOpt.get());
                            totalAmount += serviceOpt.get().getPrix();
                        } else {
                            return ReservationUtils.getResponseEntity("Service not found: ID = " + serviceId, HttpStatus.BAD_REQUEST);
                        }
                    }
                }
                reservation.setServices(selectedServices);
                reservation.setMontantTotal(totalAmount);
            }

            reservation.setDateReservation(LocalDateTime.now());
            reservationDao.save(reservation);

            return ReservationUtils.getResponseEntity("Reservation updated successfully", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteReservation(Long id) {
        try {
            Optional<Reservation> reservationOpt = reservationDao.findById(id);
            if (reservationOpt.isEmpty()) return ReservationUtils.getResponseEntity("Reservation not found", HttpStatus.NOT_FOUND);

            Reservation res = reservationOpt.get();
            User currentUser = getCurrentUser();
            if (currentUser == null) return ReservationUtils.getResponseEntity("User not found", HttpStatus.NOT_FOUND);

            if (isClient(currentUser)) {
                if (!res.getClient().getId().equals(currentUser.getId())) {
                    return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
                }
                if (res.getStatut() != ReservationStatus.PENDING) {
                    return ReservationUtils.getResponseEntity("Only PENDING reservations can be deleted by clients", HttpStatus.BAD_REQUEST);
                }
            } else if (isHotelAdmin(currentUser)) {
                if (!res.getHotel().getAdminHotelier().getId().equals(currentUser.getId())) {
                    return ReservationUtils.getResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
                }
            } else {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            reservationDao.deleteById(id);
            return ReservationUtils.getResponseEntity("Reservation deleted successfully", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------ CALCUL PRIX ------------------

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Double> calculerPrixTotalReservation(Long reservationId) {
        try {
            Optional<Reservation> reservationOpt = reservationDao.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Reservation reservation = reservationOpt.get();

            // Vérifier si la chambre existe et a un prix
            double total = 0.0;
            if (reservation.getChambre() != null && reservation.getChambre().getPrix() != null) {
                total = reservation.getChambre().getPrix();
            }

            // Ajouter le prix des services, si présents
            double total1 = reservation.getChambre().getPrix(); // prix chambre

            if (reservation.getServices() != null && !reservation.getServices().isEmpty()) {
                total1 += reservation.getServices().stream()
                        .filter(s -> s.getPrix() != null)
                        .mapToDouble(s -> s.getPrix()) // lambda au lieu de référence de méthode
                        .sum();
            }


            return ResponseEntity.ok(total);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // ------------------ CONFIRMATION / ANNULATION ------------------

    @Override
    @Transactional
    public ResponseEntity<String> confirmerReservation(Long id) {
        try {
            User user = getCurrentUser();
            if (user == null) return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);

            Optional<Reservation> resOpt = reservationDao.findById(id);
            if (resOpt.isEmpty()) return new ResponseEntity<>("Reservation not found", HttpStatus.NOT_FOUND);

            Reservation res = resOpt.get();
            if (res.getStatut() != ReservationStatus.PENDING) return new ResponseEntity<>("Only PENDING reservations can be confirmed", HttpStatus.BAD_REQUEST);

            boolean isClientAuth = isClient(user) && res.getClient().getId().equals(user.getId());
            boolean isAdminAuth = isHotelAdmin(user) && res.getHotel().getAdminHotelier().getId().equals(user.getId());
            boolean isNearStartDate = res.getDateDebut() != null && ChronoUnit.DAYS.between(LocalDate.now(), res.getDateDebut()) <= 1;

            if (!isClientAuth && !isAdminAuth && !isNearStartDate) {
                return new ResponseEntity<>("Not authorized to confirm", HttpStatus.UNAUTHORIZED);
            }

            res.setStatut(ReservationStatus.CONFIRMED);
            reservationDao.save(res);
            return new ResponseEntity<>("Reservation confirmed successfully", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> annulerReservation(Long id) {
        try {
            User user = getCurrentUser();
            if (user == null) return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);

            Optional<Reservation> resOpt = reservationDao.findById(id);
            if (resOpt.isEmpty()) return new ResponseEntity<>("Reservation not found", HttpStatus.NOT_FOUND);

            Reservation res = resOpt.get();
            if (res.getStatut() != ReservationStatus.PENDING) return new ResponseEntity<>("Only PENDING reservations can be cancelled", HttpStatus.BAD_REQUEST);

            boolean isClientAuth = isClient(user) && res.getClient().getId().equals(user.getId());
            boolean isAdminAuth = isHotelAdmin(user) && res.getHotel().getAdminHotelier().getId().equals(user.getId());

            if (!isClientAuth && !isAdminAuth) return new ResponseEntity<>("Not authorized to cancel", HttpStatus.UNAUTHORIZED);

            res.setStatut(ReservationStatus.CANCELLED);
            reservationDao.save(res);
            return new ResponseEntity<>("Reservation cancelled successfully", HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------ CONSULTATION ------------------


    @Override
    public ResponseEntity<List<ReservationWrapper>> getReservationsByHotelId(Long hotelId) {
        List<Reservation> reservations = reservationDao.findReservationsByHotelId(hotelId);
        List<ReservationWrapper> wrappers = reservations.stream()
                .map(ReservationWrapper::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(wrappers);
    }


    @Override
    public ResponseEntity<List<ReservationWrapper>> getActiveReservations() {
        List<Reservation> reservations = reservationDao.findAll().stream()
                .filter(r -> r.getStatut() == ReservationStatus.CONFIRMED)
                .collect(Collectors.toList());
        List<ReservationWrapper> wrappers = reservations.stream().map(ReservationWrapper::new).collect(Collectors.toList());
        return ResponseEntity.ok(wrappers);
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> getReservationsByClientId(Long clientId) {
        // Utiliser la méthode correcte du DAO
        List<ReservationWrapper> wrappers = reservationDao.findReservationsByClientId(clientId);
        return ResponseEntity.ok(wrappers);
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> searchReservations(Long clientId, Long hotelId,
                                                                       LocalDate dateDebut, LocalDate dateFin, String statut) {
        // Utiliser la méthode searchReservations du DAO
        List<ReservationWrapper> wrappers = reservationDao.searchReservations(clientId, hotelId, dateDebut, dateFin, statut);
        return ResponseEntity.ok(wrappers);
    }

    @Override
    public ResponseEntity<Double> getTotalAmountByClientId(Long clientId) {
        // Utiliser la méthode correcte du DAO
        Double total = com.example.pfa.reservation.repository.ReservationDAO.getTotalAmountByClientId(clientId);
        return ResponseEntity.ok(total);
    }

}
