package com.example.pfa.reservation.service;

import com.example.pfa.reservation.wrapper.UserWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface userService {


    ResponseEntity<String> signUp(Map<String, String> requestMap);

    ResponseEntity<String> login(Map<String, String> requestMap);

    ResponseEntity<List<UserWrapper>> getAllClient();

    ResponseEntity<List<UserWrapper>> getAllHotelAdmin();

    ResponseEntity<String> update(Map<String,String > requestMap);

    ResponseEntity<String> cheekToken();

    ResponseEntity<String> changePassword(Map<String, String> requestMap);

    ResponseEntity<String> forgotPassword(Map<String, String> requestMap);
}
