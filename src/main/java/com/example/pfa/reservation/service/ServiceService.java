package com.example.pfa.reservation.service;

import com.example.pfa.reservation.wrapper.ServiceWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface ServiceService {

    ResponseEntity<String> addService(Map<String, String> requestMap);

    ResponseEntity<String> updateService(Map<String, String> requestMap);

    ResponseEntity<String> deleteService(Long id);

    ResponseEntity<List<ServiceWrapper>> getAllServices();

    ResponseEntity<ServiceWrapper> getServiceById(Long id);

    ResponseEntity<List<ServiceWrapper>> getServicesByHotelId(Long hotelId);

    ResponseEntity<List<ServiceWrapper>> searchServices(String nom, Double prixMin, Double prixMax);
}
