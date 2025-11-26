package com.example.pfa.reservation.controller;

import com.example.pfa.reservation.wrapper.UserWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(path="/user")
public interface UserController {

    @PostMapping(path="/signup")
    public ResponseEntity<String> signUp(@RequestBody Map<String, String> requestMap);

    @PostMapping(path="/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> requestMap);

    @GetMapping(path="/getClient")
    public ResponseEntity<List<UserWrapper>> getAllClient();

    @GetMapping(path="/getHotelAdmin")
    public ResponseEntity<List<UserWrapper>> getAllHotelAdmin();

    @PostMapping(path="/update")
    public ResponseEntity<String> update(@RequestBody(required = true) Map<String, String> requestMap);

    @PostMapping(path="/cheekToken")
    public ResponseEntity<String> cheekToken();

    @PostMapping(path="/changePassword")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> requestMap);

    @PostMapping(path="/forgotPassword")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> requestMap);



}
