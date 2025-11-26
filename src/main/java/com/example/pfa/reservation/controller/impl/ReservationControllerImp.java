package com.example.pfa.reservation.controller.impl;

import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.controller.ReservationController;
import com.example.pfa.reservation.service.ReservationService;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.ReservationWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ReservationControllerImp implements ReservationController {

    private final ReservationService reservationService;

    // Constructor Injection
    public ReservationControllerImp(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<String> addReservation(Map<String, String> requestMap) {
        try {
            return reservationService.addReservation(requestMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(
                    ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public ResponseEntity<String> updateReservation(Map<String, String> requestMap) {
        try {
            return reservationService.updateReservation(requestMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(
                    ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public ResponseEntity<String> deleteReservation(Long id) {
        try {
            return reservationService.deleteReservation(id);
        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(
                    ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> getReservationsByClientId(Long clientId) {
        try {
            return reservationService.getReservationsByClientId(clientId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> getReservationsByHotelId(Long hotelId) {
        try {
            return reservationService.getReservationsByHotelId(hotelId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> searchReservations(Long clientId, Long hotelId, LocalDate dateDebut, LocalDate dateFin, String statut) {
        try {
            return reservationService.searchReservations(clientId, hotelId, dateDebut, dateFin, statut);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ReservationWrapper> getReservationById(Long id) {
        return null;
    }

    @Override
    public ResponseEntity<List<ReservationWrapper>> getActiveReservations() {
        try {
            return reservationService.getActiveReservations();
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Double> getTotalAmountByClientId(Long clientId) {
        try {
            return reservationService.getTotalAmountByClientId(clientId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(0.0, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Double> calculerPrixTotalReservation(Long reservationId) {
        try {
            return reservationService.calculerPrixTotalReservation(reservationId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(0.0, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> confirmerReservation(Long id) {
        try {
            return reservationService.confirmerReservation(id);
        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> annulerReservation(Long id) {
        try {
            return reservationService.annulerReservation(id);
        } catch (Exception e) {
            e.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
