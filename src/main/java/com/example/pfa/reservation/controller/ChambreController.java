package com.example.pfa.reservation.controller;

import com.example.pfa.reservation.wrapper.ChambreWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(path = "/chambre")
public interface ChambreController {
    @PostMapping(path = "/add")
    public ResponseEntity<String> addChambre(@RequestBody Map<String, String> requestMap);

    @PostMapping(path = "/update")
    public ResponseEntity<String> updateChambre(@RequestBody Map<String, String> requestMap);

    @DeleteMapping(path = "/delete/{id}")
    public ResponseEntity<String> deleteChambre(@PathVariable Long id);

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<ChambreWrapper>> getAllChambres();

    @GetMapping(path = "/getById/{id}")
    public ResponseEntity<ChambreWrapper> getChambreById(@PathVariable Long id);

    @GetMapping(path = "/getByHotelId/{hotelId}")
    public ResponseEntity<List<ChambreWrapper>> getChambresByHotelId(@PathVariable Long hotelId);

    @GetMapping(path = "/search")
    public ResponseEntity<List<ChambreWrapper>> searchChambres(
            @RequestParam(required = false) String typeChambre,
            @RequestParam(required = false) Double prixMin,
            @RequestParam(required = false) Double prixMax,
            @RequestParam(required = false) Boolean disponibilite
    );

    @GetMapping(path = "/getAvailableByHotelId/{hotelId}")
    public ResponseEntity<List<ChambreWrapper>> getAvailableChambresByHotelId(@PathVariable Long hotelId);

}