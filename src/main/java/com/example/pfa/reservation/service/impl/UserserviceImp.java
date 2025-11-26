package com.example.pfa.reservation.service.impl;

import com.example.pfa.reservation.constants.ReservationConstants;
import com.example.pfa.reservation.jwt.CustomerUsersDetailsService;
import com.example.pfa.reservation.jwt.JwtFilter;
import com.example.pfa.reservation.jwt.JwtUtil;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.service.UserService;
import com.example.pfa.reservation.utils.EmailUtils;
import com.example.pfa.reservation.utils.ReservationUtils;
import com.example.pfa.reservation.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserserviceImp implements UserService {

    @Autowired
    private UserDAO userDao;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomerUsersDetailsService customerUsersDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailUtils emailUtils;

    // ---------------- SIGNUP ----------------
    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("Inside signup{}", requestMap);
        try {
            if (!validateSignUpMap(requestMap)) {
                return ReservationUtils.getResponseEntity(ReservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            String email = requestMap.get("email");
            String userName = requestMap.get("userName");

            User existingUser = userDao.findByEmailOrUsername(email, userName);
            if (existingUser != null) {
                if (existingUser.getEmail().equals(email)) {
                    return ReservationUtils.getResponseEntity("Email already exists", HttpStatus.BAD_REQUEST);
                } else {
                    return ReservationUtils.getResponseEntity("Username already exists", HttpStatus.BAD_REQUEST);
                }
            }

            User newUser = getUserFromMap(requestMap);
            userDao.save(newUser);
            return ReservationUtils.getResponseEntity("Successfully Registered.", HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error during signup", ex);
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateSignUpMap(Map<String, String> requestMap) {
        return requestMap.containsKey("nom") &&
                requestMap.containsKey("prenom") &&
                requestMap.containsKey("email") &&
                requestMap.containsKey("userName") &&
                requestMap.containsKey("password") &&
                requestMap.containsKey("telephone");
    }

    private User getUserFromMap(Map<String, String> requestMap) {
        User user = new User();
        user.setNom(requestMap.get("nom"));
        user.setPrenom(requestMap.get("prenom"));
        user.setEmail(requestMap.get("email"));
        user.setUserName(requestMap.get("userName"));
        user.setTelephone(requestMap.get("telephone"));
        user.setPassword(passwordEncoder.encode(requestMap.get("password")));

        String roleStr = requestMap.get("role");
        if (roleStr != null) {
            user.setRole(Role.valueOf(roleStr));
        }
        return user;
    }

    // ---------------- LOGIN ----------------
    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");

        if (!requestMap.containsKey("identifier") || !requestMap.containsKey("password")) {
            return new ResponseEntity<>("{\"message\":\"Identifier and Password are required.\"}", HttpStatus.BAD_REQUEST);
        }

        String identifier = requestMap.get("identifier");
        String password = requestMap.get("password");

        try {
            UserDetails userDetails = customerUsersDetailsService.loadUserByUsername(identifier);

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), password)
            );

            if (!auth.isAuthenticated()) {
                return new ResponseEntity<>("{\"message\":\"Authentication failed.\"}", HttpStatus.UNAUTHORIZED);
            }

            User fullUser = userDao.findByEmail(userDetails.getUsername());
            if (fullUser == null) fullUser = userDao.findByUserName(userDetails.getUsername());
            if (fullUser == null) return new ResponseEntity<>("{\"message\":\"User details not found.\"}", HttpStatus.BAD_REQUEST);

            String role = fullUser.getRole() != null ? fullUser.getRole().toString() : "";
            String token = jwtUtil.generateToken(fullUser.getEmail(), role);

            return new ResponseEntity<>("{\"token\":\"" + token + "\"}", HttpStatus.OK);

        } catch (BadCredentialsException ex) {
            log.error("Invalid credentials: {}", ex.getMessage());
            return new ResponseEntity<>("{\"message\":\"Bad Credentials.\"}", HttpStatus.UNAUTHORIZED);
        } catch (UsernameNotFoundException ex) {
            log.error("User not found: {}", ex.getMessage());
            return new ResponseEntity<>("{\"message\":\"User not found.\"}", HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            log.error("Error during authentication: {}", ex.getMessage());
            return new ResponseEntity<>("{\"message\":\"An error occurred during authentication.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ---------------- GET ALL CLIENTS ----------------
    @Override
    public ResponseEntity<List<UserWrapper>> getAllClient() {
        try {
            if (jwtFilter.isGlobalAdmin() || jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(userDao.getAllClient(), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            log.error("Error getting all clients", ex);
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ---------------- GET ALL HOTEL ADMINS ----------------
    @Override
    public ResponseEntity<List<UserWrapper>> getAllHotelAdmin() {
        try {
            if (jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(userDao.getAllHotelAdmin(), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            log.error("Error getting all hotel admins", ex);
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ---------------- UPDATE USER ----------------
    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (!jwtFilter.isHotelAdmin() && !jwtFilter.isClient()) {
                return ReservationUtils.getResponseEntity(ReservationConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            Optional<User> optional = userDao.findById(Long.parseLong(requestMap.get("id")));
            if (optional.isEmpty()) {
                return ReservationUtils.getResponseEntity("User id does not exist", HttpStatus.BAD_REQUEST);
            }

            userDao.updateProfil(
                    requestMap.get("nom"),
                    requestMap.get("prenom"),
                    requestMap.get("email"),
                    requestMap.get("userName"),
                    requestMap.get("telephone"),
                    Long.parseLong(requestMap.get("id"))
            );

            sendMailToAllAdmin(optional.get().getEmail(), userDao.getAllGlobalAdmin());
            return ReservationUtils.getResponseEntity("User Profil Updated Successfully", HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error updating user", ex);
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendMailToAllAdmin(String email, List<String> allGlobalAdmin) {
        allGlobalAdmin.remove(jwtFilter.getCurrentUserRole());
        emailUtils.sendSimpleMessage(
                jwtFilter.getCurrentUserRole(),
                "Account Approved",
                "User: " + email + " is approved by Admin: " + jwtFilter.getCurrentUserRole(),
                allGlobalAdmin
        );
    }

    // ---------------- CHECK TOKEN ----------------
    @Override
    public ResponseEntity<String> cheekToken() {
        return ReservationUtils.getResponseEntity("true", HttpStatus.OK);
    }

    // ---------------- CHANGE PASSWORD ----------------
    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            String identifier = jwtFilter.getCurrentUserRole();
            User userObj = userDao.findByEmail(identifier);
            if (userObj == null) userObj = userDao.findByUserName(identifier);

            if (userObj != null) {
                if (passwordEncoder.matches(requestMap.get("oldPassword"), userObj.getPassword())) {
                    userObj.setPassword(passwordEncoder.encode(requestMap.get("newPassword")));
                    userDao.save(userObj);
                    return ReservationUtils.getResponseEntity("Password updated successfully", HttpStatus.OK);
                } else {
                    return ReservationUtils.getResponseEntity("Incorrect Old Password", HttpStatus.BAD_REQUEST);
                }
            }

            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception ex) {
            log.error("Error changing password", ex);
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ---------------- FORGOT PASSWORD ----------------
    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            String identifier = jwtFilter.getCurrentUserRole();
            User userObj = userDao.findByEmail(identifier);
            if (userObj == null) userObj = userDao.findByUserName(identifier);

            if (userObj != null && !Strings.isEmpty(userObj.getEmail())) {
                emailUtils.forgotMail(userObj.getEmail(), "Credentials by Hotels Management System", userObj.getPassword());
            }

            return ReservationUtils.getResponseEntity("Check your mail for credentials.", HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Error in forgot password", ex);
            return ReservationUtils.getResponseEntity(ReservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
