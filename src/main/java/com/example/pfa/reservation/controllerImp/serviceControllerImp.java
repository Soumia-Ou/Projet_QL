package com.example.pfa.reservation.controllerImp;

import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.controller.serviceController;
import com.example.pfa.reservation.service.serviceService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.ServiceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class serviceControllerImp implements serviceController {

    @Autowired
    private serviceService serviceService;

    @Override
    public ResponseEntity<String> addService(Map<String, String> requestMap) {
        try {
            return serviceService.addService(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateService(Map<String, String> requestMap) {
        try {
            return serviceService.updateService(requestMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> deleteService(Long id) {
        try {
            return serviceService.deleteService(id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ServiceWrapper>> getAllServices() {
        try {
            return serviceService.getAllServices();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<ServiceWrapper> getServiceById(Long id) {
        try {
            return serviceService.getServiceById(id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ServiceWrapper(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ServiceWrapper>> getServicesByHotelId(Long hotelId) {
        try {
            return serviceService.getServicesByHotelId(hotelId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ServiceWrapper>> searchServices(String nom, Double prixMin, Double prixMax) {
        try {
            return serviceService.searchServices(nom, prixMin, prixMax);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
