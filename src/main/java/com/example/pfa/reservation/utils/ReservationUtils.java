package com.example.pfa.reservation.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ReservationUtils {

    private ReservationUtils() {
        // Private constructor to prevent instantiation
    }

    public static ResponseEntity<String> getResponseEntity(String responseMessage, HttpStatus httpStatus) {
        // Utilisation du diamond operator <>
        return new ResponseEntity<>("{\"message\":\"" + responseMessage + "\"}", httpStatus);
    }
}
