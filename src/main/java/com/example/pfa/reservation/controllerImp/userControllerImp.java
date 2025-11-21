package com.example.pfa.reservation.controllerImp;

import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.controller.userController;
import com.example.pfa.reservation.service.userService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.UserWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class userControllerImp implements userController {

    @Autowired
    userService userservice;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        try {
            System.out.println("Données reçues : " + requestMap);
            return userservice.signUp(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        try {
            return userservice.login(requestMap);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllClient() {
        try{
            return userservice.getAllClient();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<List<UserWrapper>>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllHotelAdmin() {
        try{
            return userservice.getAllHotelAdmin();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<List<UserWrapper>>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            return userservice.update(requestMap);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> cheekToken() {
        try{
            return userservice.cheekToken();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try{
            return userservice.changePassword(requestMap);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try{
            return userservice.forgotPassword(requestMap);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

    }


}
