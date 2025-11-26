package com.example.pfa.reservation.service.impl;

import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.repository.ServiceDAO;
import com.example.pfa.reservation.repository.HotelDAO;
import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.model.Service;
import com.example.pfa.reservation.model.Hotel;
import com.example.pfa.reservation.service.ServiceService;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.ServiceWrapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

@Slf4j
@org.springframework.stereotype.Service
public class ServiceServiceImp implements ServiceService {

    @Autowired
    private ServiceDAO serviceDao;

    @Autowired
    private HotelDAO hotelDao;

    @Autowired
    private JwtFilter jwtFilter;

    private boolean validateServiceMap(Map<String, String> requestMap) {
        return requestMap.containsKey("nom") &&
                requestMap.containsKey("description") &&
                requestMap.containsKey("prix") &&
                requestMap.containsKey("hotelId");
    }

    @Override
    public ResponseEntity<String> addService(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!validateServiceMap(requestMap)) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            Service service = new Service();
            service.setNom(requestMap.get("nom"));
            service.setDescription(requestMap.get("description"));
            try {
                service.setPrix(Double.parseDouble(requestMap.get("prix")));
            } catch (NumberFormatException ex) {
                return ReservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get("hotelId"));
            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            Hotel hotel = hotelOpt.get();

            // Récupérer le username ou email du user connecté
            String currentLogin = jwtFilter.getCurrentUserRole();

            // Vérifier si l'utilisateur est bien l'admin de l'hôtel
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return ReservationUtils.getResponseEntity("Unauthorized to add a service to this hotel", HttpStatus.UNAUTHORIZED);
            }

            service.setHotel(hotel);

            serviceDao.save(service);
            return ReservationUtils.getResponseEntity("Service added successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> updateService(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!requestMap.containsKey("id")) {
                return ReservationUtils.getResponseEntity("Service ID is required", HttpStatus.BAD_REQUEST);
            }

            Long serviceId = Long.parseLong(requestMap.get("id"));
            Optional<Service> serviceOpt = serviceDao.findById(serviceId);

            if (serviceOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Service not found", HttpStatus.NOT_FOUND);
            }

            Service existingService = serviceOpt.get();
            Hotel associatedHotel = existingService.getHotel();

            // Vérifier que l'utilisateur connecté est bien l'admin de cet hôtel
            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return ReservationUtils.getResponseEntity("Unauthorized to update a service of this hotel", HttpStatus.UNAUTHORIZED);
            }

            if (!validateServiceMap(requestMap)) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            existingService.setNom(requestMap.get("nom"));
            existingService.setDescription(requestMap.get("description"));

            try {
                existingService.setPrix(Double.parseDouble(requestMap.get("prix")));
            } catch (NumberFormatException ex) {
                return ReservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get("hotelId"));
            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }
            existingService.setHotel(hotelOpt.get());

            serviceDao.save(existingService);
            return ReservationUtils.getResponseEntity("Service updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteService(Long id) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            Optional<Service> serviceOpt = serviceDao.findById(id);
            if (serviceOpt.isEmpty()) {
                return ReservationUtils.getResponseEntity("Service not found", HttpStatus.NOT_FOUND);
            }

            Service existingService = serviceOpt.get();
            Hotel associatedHotel = existingService.getHotel();

            // Vérifier que l'utilisateur connecté est bien l'admin de cet hôtel
            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return ReservationUtils.getResponseEntity("Unauthorized to delete a service of this hotel", HttpStatus.UNAUTHORIZED);
            }

            serviceDao.deleteById(id);
            return ReservationUtils.getResponseEntity("Service deleted successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ServiceWrapper>> getAllServices() {
        try {
            if (!jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            List<ServiceWrapper> services = serviceDao.getAllServices();
            if (services == null || services.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(services, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ServiceWrapper> getServiceById(Long id) {
        try {
            // Vérifier si l'utilisateur connecté est un admin hôtelier
            if (!jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            // Récupérer le service par ID
            Optional<ServiceWrapper> serviceOpt = serviceDao.getServiceById(id);
            if (serviceOpt.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            ServiceWrapper service = serviceOpt.get();

            // Vérifier si l'admin hôtelier connecté a accès à ce service
            Long hotelId = service.getHotelId(); // Récupérer l'ID de l'hôtel du service
            String currentLogin = jwtFilter.getCurrentUserRole(); // Récupérer le login de l'utilisateur connecté

            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            Hotel hotel = hotelOpt.get();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            // Vérifier que l'utilisateur connecté est bien l'admin de l'hôtel
            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            return new ResponseEntity<>(service, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ServiceWrapper>> getServicesByHotelId(Long hotelId) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            Optional<Hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            Hotel hotel = hotelOpt.get();

            String currentLogin = jwtFilter.getCurrentUserRole();
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            List<ServiceWrapper> services = serviceDao.getServicesByHotelId(hotelId);
            if (services == null || services.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(services, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ServiceWrapper>> searchServices(String nom, Double prixMin, Double prixMax) {
        try {
            if (jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }

            List<ServiceWrapper> services = serviceDao.searchServices(nom, prixMin, prixMax);
            if (services == null || services.isEmpty()) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(services, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
