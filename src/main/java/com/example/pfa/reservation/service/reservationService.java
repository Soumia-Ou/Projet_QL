package com.example.pfa.reservation.service;

import com.example.pfa.reservation.wrapper.ReservationWrapper;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface reservationService {
    ResponseEntity<String> addReservation(Map<String, String> requestMap);

    ResponseEntity<String> updateReservation(Map<String, String> requestMap);

    ResponseEntity<String> deleteReservation(Long id);

    ResponseEntity<List<ReservationWrapper>> getReservationsByClientId(Long clientId);

    ResponseEntity<List<ReservationWrapper>> getReservationsByHotelId(Long hotelId);

    ResponseEntity<List<ReservationWrapper>> searchReservations(Long clientId, Long hotelId, LocalDate dateDebut, LocalDate dateFin, String statut);

    ResponseEntity<List<ReservationWrapper>> getActiveReservations();

    ResponseEntity<Double> getTotalAmountByClientId(Long clientId);

    ResponseEntity<Double> calculerPrixTotalReservation(Long reservationId);

    ResponseEntity<String> confirmerReservation(Long id);

    ResponseEntity<String> annulerReservation(Long id);

}
