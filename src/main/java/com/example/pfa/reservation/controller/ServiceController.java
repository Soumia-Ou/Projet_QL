package com.example.pfa.reservation.controller;

import com.example.pfa.reservation.wrapper.ServiceWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(path = "/service")
public interface ServiceController {

    @PostMapping(path = "/add")
    public ResponseEntity<String> addService(@RequestBody Map<String, String> requestMap);

    @PostMapping(path = "/update")
    public ResponseEntity<String> updateService(@RequestBody Map<String, String> requestMap);

    @DeleteMapping(path = "/delete/{id}")
    public ResponseEntity<String> deleteService(@PathVariable Long id);

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<ServiceWrapper>> getAllServices();

    @GetMapping(path = "/getById/{id}")
    public ResponseEntity<ServiceWrapper> getServiceById(@PathVariable Long id);

    @GetMapping(path = "/getByHotelId/{hotelId}")
    public ResponseEntity<List<ServiceWrapper>> getServicesByHotelId(@PathVariable Long hotelId);

    @GetMapping(path = "/search")
    public ResponseEntity<List<ServiceWrapper>> searchServices(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) Double prixMin,
            @RequestParam(required = false) Double prixMax
    );

}
