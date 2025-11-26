package com.example.pfa.reservation.controller.impl;

import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.controller.UserController;
import com.example.pfa.reservation.service.UserService;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.UserWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class UserControllerImp implements UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserControllerImp.class);

    private final UserService userservice;

    //  Constructor Injection (Recommandé par Spring + SonarQube)
    public UserControllerImp(UserService userservice) {
        this.userservice = userservice;
    }

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        try {
            logger.info("Données reçues pour signUp : {}", requestMap);
            return userservice.signUp(requestMap);
        } catch (Exception ex) {
            logger.error("Erreur lors du signUp", ex);
        }
        return ReservationUtils.getResponseEntity(
                ReservationConstants.SOMETHING_WENT_WRONG,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        try {
            return userservice.login(requestMap);
        } catch (Exception ex) {
            logger.error("Erreur lors du login", ex);
        }
        return ReservationUtils.getResponseEntity(
                ReservationConstants.SOMETHING_WENT_WRONG,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllClient() {
        try {
            return userservice.getAllClient();
        } catch (Exception ex) {
            logger.error("Erreur lors de getAllClient", ex);
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllHotelAdmin() {
        try {
            return userservice.getAllHotelAdmin();
        } catch (Exception ex) {
            logger.error("Erreur lors de getAllHotelAdmin", ex);
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            return userservice.update(requestMap);
        } catch (Exception ex) {
            logger.error("Erreur lors de update", ex);
        }
        return ReservationUtils.getResponseEntity(
                ReservationConstants.SOMETHING_WENT_WRONG,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> cheekToken() {
        try {
            return userservice.cheekToken();
        } catch (Exception ex) {
            logger.error("Erreur lors de cheekToken", ex);
        }
        return ReservationUtils.getResponseEntity(
                ReservationConstants.SOMETHING_WENT_WRONG,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            return userservice.changePassword(requestMap);
        } catch (Exception ex) {
            logger.error("Erreur lors de changePassword", ex);
        }
        return ReservationUtils.getResponseEntity(
                ReservationConstants.SOMETHING_WENT_WRONG,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            return userservice.forgotPassword(requestMap);
        } catch (Exception ex) {
            logger.error("Erreur lors de forgotPassword", ex);
        }
        return ReservationUtils.getResponseEntity(
                ReservationConstants.SOMETHING_WENT_WRONG,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
