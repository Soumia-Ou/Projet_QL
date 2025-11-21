package com.example.pfa.reservation.controller;

import com.example.pfa.reservation.wrapper.ReservationWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RequestMapping(path = "/reservation")
public interface reservationController {

    @PostMapping(path = "/add")
    ResponseEntity<String> addReservation(@RequestBody Map<String, String> requestMap);

    @PostMapping(path = "/update")
    ResponseEntity<String> updateReservation(@RequestBody Map<String, String> requestMap);

    @DeleteMapping(path = "/delete/{id}")
    ResponseEntity<String> deleteReservation(@PathVariable Long id);

    @GetMapping(path = "/getByClientId/{clientId}")
    ResponseEntity<List<ReservationWrapper>> getReservationsByClientId(@PathVariable Long clientId);

    @GetMapping(path = "/getByHotelId/{hotelId}")
    ResponseEntity<List<ReservationWrapper>> getReservationsByHotelId(@PathVariable Long hotelId);

    @GetMapping(path = "/getAllActive")
    ResponseEntity<List<ReservationWrapper>> getActiveReservations();

    @PutMapping(path = "/confirm/{id}")
    ResponseEntity<String> confirmerReservation(@PathVariable Long id);

    @PutMapping(path = "/cancel/{id}")
    ResponseEntity<String> annulerReservation(@PathVariable Long id);

    @GetMapping(path = "/totalAmount/{clientId}")
    ResponseEntity<Double> getTotalAmountByClientId(@PathVariable Long clientId);

    @GetMapping(path = "/prixTotal/{id}")
    ResponseEntity<Double> calculerPrixTotalReservation(@PathVariable Long id);

    @GetMapping(path = "/search")
    ResponseEntity<List<ReservationWrapper>> searchReservations(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) String statut
    );

    @GetMapping(path = "/getById/{id}")
    ResponseEntity<ReservationWrapper> getReservationById(@PathVariable Long id);
}
