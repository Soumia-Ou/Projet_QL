package com.example.pfa.reservation.controller.impl;

import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.controller.HotelController;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.HotelWrapper;
import com.example.pfa.reservation.service.HotelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class HotelControllerImp implements HotelController {

    private final HotelService hotelservice;

    //  Injection par constructeur (recommandée)
    public HotelControllerImp(HotelService hotelservice) {
        this.hotelservice = hotelservice;
    }

    @Override
    public ResponseEntity<String> addHotel(Map<String, String> requestMap) {
        try {
            return hotelservice.addHotel(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> updateHotel(Map<String, String> requestMap) {
        try {
            return hotelservice.updateHotel(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> getAllHotels() {
        try {
            return hotelservice.getAllHotels();
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<HotelWrapper> getHotelById(Long id) {
        try {
            return hotelservice.getHotelById(id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new HotelWrapper(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> deleteHotel(Long id) {
        try {
            return hotelservice.deleteHotel(id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> getHotelsByAdminId(Long adminId) {
        try {
            return hotelservice.getHotelsByAdminId(adminId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<HotelWrapper>> searchHotels(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) Integer etoiles) {

        try {
            return hotelservice.searchHotels(nom, adresse, etoiles);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
