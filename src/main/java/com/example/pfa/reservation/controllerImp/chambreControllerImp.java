package com.example.pfa.reservation.controllerImp;

import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.controller.chambreController;
import com.example.pfa.reservation.service.chambreService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.ChambreWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class chambreControllerImp implements chambreController {

    @Autowired
    private chambreService chambreService;

    @Override
    public ResponseEntity<String> addChambre(Map<String, String> requestMap) {
        try {
            return chambreService.addChambre(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateChambre(Map<String, String> requestMap) {
        try {
            return chambreService.updateChambre(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> deleteChambre(Long id) {
        try {
            return chambreService.deleteChambre(id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> getAllChambres() {
        try {
            return chambreService.getAllChambres();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<ChambreWrapper> getChambreById(Long id) {
        try {
            return chambreService.getChambreById(id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ChambreWrapper(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> getChambresByHotelId(Long hotelId) {
        try {
            return chambreService.getChambresByHotelId(hotelId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> searchChambres(String typeChambre, Double prixMin, Double prixMax, Boolean disponibilite) {
        try {
            return chambreService.searchChambres(typeChambre, prixMin, prixMax, disponibilite);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ChambreWrapper>> getAvailableChambresByHotelId(Long hotelId) {
        try {
            return chambreService.getAvailableChambresByHotelId(hotelId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
