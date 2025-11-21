package com.example.pfa.reservation.serviceImp;

import com.example.pfa.reservation.JWT.JwtFilter;
import com.example.pfa.reservation.Repository.serviceDAO;
import com.example.pfa.reservation.Repository.hotelDAO;
import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.model.service;
import com.example.pfa.reservation.model.hotel;
import com.example.pfa.reservation.service.serviceService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.wrapper.ServiceWrapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class serviceServiceImp implements serviceService {

    @Autowired
    private serviceDAO serviceDao;

    @Autowired
    private hotelDAO hotelDao;

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
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!validateServiceMap(requestMap)) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            service service = new service();
            service.setNom(requestMap.get("nom"));
            service.setDescription(requestMap.get("description"));
            try {
                service.setPrix(Double.parseDouble(requestMap.get("prix")));
            } catch (NumberFormatException ex) {
                return reservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get("hotelId"));
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }

            hotel hotel = hotelOpt.get();

            // Récupérer le username ou email du user connecté
            String currentLogin = jwtFilter.getCurreentUser();

            // Vérifier si l'utilisateur est bien l'admin de l'hôtel
            String adminEmail = hotel.getAdminHotelier().getEmail();
            String adminUsername = hotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return reservationUtils.getResponseEntity("Unauthorized to add a service to this hotel", HttpStatus.UNAUTHORIZED);
            }

            service.setHotel(hotel);

            serviceDao.save(service);
            return reservationUtils.getResponseEntity("Service added successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> updateService(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            if (!requestMap.containsKey("id")) {
                return reservationUtils.getResponseEntity("Service ID is required", HttpStatus.BAD_REQUEST);
            }

            Long serviceId = Long.parseLong(requestMap.get("id"));
            Optional<service> serviceOpt = serviceDao.findById(serviceId);

            if (serviceOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Service not found", HttpStatus.NOT_FOUND);
            }

            service existingService = serviceOpt.get();
            hotel associatedHotel = existingService.getHotel();

            // Vérifier que l'utilisateur connecté est bien l'admin de cet hôtel
            String currentLogin = jwtFilter.getCurreentUser();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return reservationUtils.getResponseEntity("Unauthorized to update a service of this hotel", HttpStatus.UNAUTHORIZED);
            }

            if (!validateServiceMap(requestMap)) {
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            existingService.setNom(requestMap.get("nom"));
            existingService.setDescription(requestMap.get("description"));

            try {
                existingService.setPrix(Double.parseDouble(requestMap.get("prix")));
            } catch (NumberFormatException ex) {
                return reservationUtils.getResponseEntity("Invalid price value", HttpStatus.BAD_REQUEST);
            }

            Long hotelId = Long.parseLong(requestMap.get("hotelId"));
            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Hotel not found", HttpStatus.NOT_FOUND);
            }
            existingService.setHotel(hotelOpt.get());

            serviceDao.save(existingService);
            return reservationUtils.getResponseEntity("Service updated successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteService(Long id) {
        try {
            if (!jwtFilter.isHotelAdmin()) {
                return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            Optional<service> serviceOpt = serviceDao.findById(id);
            if (serviceOpt.isEmpty()) {
                return reservationUtils.getResponseEntity("Service not found", HttpStatus.NOT_FOUND);
            }

            service existingService = serviceOpt.get();
            hotel associatedHotel = existingService.getHotel();

            // Vérifier que l'utilisateur connecté est bien l'admin de cet hôtel
            String currentLogin = jwtFilter.getCurreentUser();
            String adminEmail = associatedHotel.getAdminHotelier().getEmail();
            String adminUsername = associatedHotel.getAdminHotelier().getUserName();

            if (!(currentLogin.equalsIgnoreCase(adminEmail) || currentLogin.equalsIgnoreCase(adminUsername))) {
                return reservationUtils.getResponseEntity("Unauthorized to delete a service of this hotel", HttpStatus.UNAUTHORIZED);
            }

            serviceDao.deleteById(id);
            return reservationUtils.getResponseEntity("Service deleted successfully", HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
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
            String currentLogin = jwtFilter.getCurreentUser(); // Récupérer le login de l'utilisateur connecté

            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            hotel hotel = hotelOpt.get();
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

            Optional<hotel> hotelOpt = hotelDao.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }

            hotel hotel = hotelOpt.get();

            String currentLogin = jwtFilter.getCurreentUser();
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
