package com.example.pfa.reservation.service;

import com.example.pfa.reservation.wrapper.HotelWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface HotelService {
    ResponseEntity<String> addHotel(Map<String, String> requestMap);

    ResponseEntity<String> updateHotel(Map<String, String> requestMap);

    ResponseEntity<List<HotelWrapper>> getAllHotels();

    ResponseEntity<HotelWrapper> getHotelById(Long id);

    ResponseEntity<String> deleteHotel(Long id);

    ResponseEntity<List<HotelWrapper>> getHotelsByAdminId(Long adminId);

    ResponseEntity<List<HotelWrapper>> searchHotels(String nom, String adresse, Integer etoiles);

}
