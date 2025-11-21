package com.example.pfa.reservation.serviceImp;

import com.example.pfa.reservation.JWT.CustomerUsersDetailsService;
import com.example.pfa.reservation.JWT.JwtFilter;
import com.example.pfa.reservation.JWT.JwtUtil;
import com.example.pfa.reservation.Repository.userDAO;
import com.example.pfa.reservation.constants.reservationConstants;
import com.example.pfa.reservation.model.Role;
import com.example.pfa.reservation.model.user;
import com.example.pfa.reservation.service.userService;
import com.example.pfa.reservation.utils.reservationUtils;
import com.example.pfa.reservation.utils.emailUtils;
import com.example.pfa.reservation.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
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

import static jdk.dynalink.linker.support.Guards.isNull;

@Slf4j //logging propre et efficace
@Service
public class userServiceImp implements userService {

    @Autowired
    userDAO userDao;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    CustomerUsersDetailsService customerUsersDetailsService;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    emailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("Inside signup{}",requestMap);
        try{
            if(validateSignUpMap((requestMap))){
                String email = requestMap.get("email");
                String userName = requestMap.get("userName");

                //verifie si email ou username existent
                user user = userDao.findByEmailOrUsername(email,userName);

                if (Objects.nonNull(user)) {
                    if (user.getEmail().equals(email)) {
                        return reservationUtils.getResponseEntity("Email already exists", HttpStatus.BAD_REQUEST);
                    } else {
                        return reservationUtils.getResponseEntity("Username already exists", HttpStatus.BAD_REQUEST);
                    }
                }

                // Si l'utilisateur n'existe pas, on l'enregistre
                user newuser = getUserFromMap(requestMap);
                userDao.save(newuser);
                return reservationUtils.getResponseEntity("Successfully Registred .", HttpStatus.OK);

            }
            else{
                return reservationUtils.getResponseEntity(reservationConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        }catch(Exception ex){
            log.error("Error during signup", ex);
            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG ,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateSignUpMap(Map<String, String> requestMap){
        if(requestMap.containsKey("nom") && requestMap.containsKey("prenom") &&
                requestMap.containsKey("email") && requestMap.containsKey("userName") &&
                requestMap.containsKey("password") && requestMap.containsKey("telephone")){
            return true;
        }
        return false;
    }

    private user getUserFromMap(Map<String, String> requestMap) {
        user user = new user();
        user.setNom(requestMap.get("nom"));
        user.setPrenom(requestMap.get("prenom"));
        user.setEmail(requestMap.get("email"));
        user.setUserName(requestMap.get("userName"));
        user.setTelephone(requestMap.get("telephone"));

        // ENCODER LE MOT DE PASSE ICI !!!
        String rawPassword = requestMap.get("password");
        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(encodedPassword);

        String roleStr = requestMap.get("role");
        if (roleStr != null) {
            user.setRole(Role.valueOf(roleStr)); // Convertir String en Role directement
        }
        return user;
    }


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

            if (auth.isAuthenticated()) {
                // ✅ AJOUTÉ : récupération des détails complets de l'utilisateur
                com.example.pfa.reservation.model.user fullUserDetails = customerUsersDetailsService.getUserDetail();

                if (fullUserDetails == null) {
                    return new ResponseEntity<>("{\"message\":\"User details not found.\"}", HttpStatus.BAD_REQUEST);
                }

                String role = fullUserDetails.getRole() != null ? fullUserDetails.getRole().toString() : "";

                switch (role) {
                    case "GLOBAL_ADMIN":
                    case "HOTEL_ADMIN":
                    case "CLIENT":
                        return new ResponseEntity<>(
                                "{\"token\":\"" + jwtUtil.generateToken(fullUserDetails.getEmail(), role) + "\"}", HttpStatus.OK);
                    default:
                        return new ResponseEntity<>("{\"message\":\"Unknown Role\"}", HttpStatus.BAD_REQUEST);
                }
            }

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

        return new ResponseEntity<>("{\"message\":\"An error occurred during authentication.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    public ResponseEntity<List<UserWrapper>> getAllClient() {
        try {
            if (jwtFilter.isGlobalAdmin() || jwtFilter.isHotelAdmin()) {
                return new ResponseEntity<>(userDao.getAllClient(), HttpStatus.OK);
            } else{
                return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllHotelAdmin() {

        try {
            if (jwtFilter.isGlobalAdmin()) {
                return new ResponseEntity<>(userDao.getAllHotelAdmin(), HttpStatus.OK);
            } else{
                return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isHotelAdmin() || jwtFilter.isClient()) {
                Optional<user> optional = userDao.findById(Long.parseLong(requestMap.get("id")));
                if (!optional.isEmpty()){
                    userDao.updateProfil(requestMap.get("nom"),requestMap.get("prenom"),requestMap.get("email"),
                            requestMap.get("userName"),requestMap.get("telephone"),Long.parseLong(requestMap.get("id")));
                    sendMailToAllAdmin(optional.get().getEmail(),userDao.getAllGlobalAdmin());
                    return reservationUtils.getResponseEntity("User Profil Updated Successfully",HttpStatus.OK);

                }else {
                    return reservationUtils.getResponseEntity("User id does not exist",HttpStatus.OK);
                }
            } else{
              return reservationUtils.getResponseEntity(reservationConstants.UNAUTHORIZED_ACCESS,HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendMailToAllAdmin(String email, List<String> allGlobalAdmin) {
        allGlobalAdmin.remove(jwtFilter.getCurreentUser());
        emailUtils.sendSimpleMessage(jwtFilter.getCurreentUser(),"Account Approoved","User:- " + email + "\n is approved by \n Admin:-" + jwtFilter.getCurreentUser() , allGlobalAdmin);

    }

    @Override
    public ResponseEntity<String> cheekToken() {

        return reservationUtils.getResponseEntity("true",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            String identifier = jwtFilter.getCurreentUser();
            user userObj = userDao.findByEmail(identifier);
            if (userObj == null) {
                userObj = userDao.findByUserName(identifier);
            }

            if (userObj != null) {
                if (passwordEncoder.matches(requestMap.get("oldPassword"), userObj.getPassword())) {
                    userObj.setPassword(passwordEncoder.encode(requestMap.get("newPassword")));
                    userDao.save(userObj);
                    return reservationUtils.getResponseEntity("Password updated successfully", HttpStatus.OK);
                }
                return reservationUtils.getResponseEntity("Incorrect Old Password", HttpStatus.BAD_REQUEST);
            }

            return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try{
            String identifier = jwtFilter.getCurreentUser();
            user userObj = userDao.findByEmail(identifier);
            if (userObj == null) {
                userObj = userDao.findByUserName(identifier);
            }
            if (!Objects.isNull(userObj) && !Strings.isEmpty(userObj.getEmail()))
                emailUtils.forgotMail(userObj.getEmail(),"Credentials by Hotels Management System",userObj.getPassword());
            return reservationUtils.getResponseEntity("Check your mail for credentials.", HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return reservationUtils.getResponseEntity(reservationConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
