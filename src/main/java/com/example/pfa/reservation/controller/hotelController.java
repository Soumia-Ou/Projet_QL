package com.example.pfa.reservation.controller;

import com.example.pfa.reservation.wrapper.HotelWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(path="/hotel")
public interface hotelController {

    @PostMapping(path="/add")
    public ResponseEntity<String> addHotel(@RequestBody Map<String, String> requestMap);

    @PostMapping(path="/update")
    public ResponseEntity<String> updateHotel(@RequestBody Map<String, String> requestMap);

    @DeleteMapping(path="/delete/{id}")
    public ResponseEntity<String> deleteHotel(@PathVariable Long id);

    @GetMapping(path="/getAll")
    public ResponseEntity<List<HotelWrapper>> getAllHotels();

    @GetMapping(path="/getById/{id}")
    public ResponseEntity<HotelWrapper> getHotelById(@PathVariable Long id);

    @GetMapping(path="/getByAdminId/{adminId}")
    public ResponseEntity<List<HotelWrapper>> getHotelsByAdminId(@PathVariable Long adminId);

    @GetMapping(path="/search")
    public ResponseEntity<List<HotelWrapper>> searchHotels(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) Integer etoiles);
}