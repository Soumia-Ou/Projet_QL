package com.example.pfa.reservation.service;

import com.example.pfa.reservation.wrapper.ChambreWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface chambreService {

    ResponseEntity<String> addChambre(Map<String, String> requestMap);

    ResponseEntity<String> updateChambre(Map<String, String> requestMap);

    ResponseEntity<String> deleteChambre(Long id);

    ResponseEntity<List<ChambreWrapper>> getAllChambres();

    ResponseEntity<ChambreWrapper> getChambreById(Long id);

    ResponseEntity<List<ChambreWrapper>> getChambresByHotelId(Long hotelId);

    ResponseEntity<List<ChambreWrapper>> searchChambres(String typeChambre, Double prixMin, Double prixMax, Boolean disponibilite);

    ResponseEntity<List<ChambreWrapper>> getAvailableChambresByHotelId(Long hotelId);
}
