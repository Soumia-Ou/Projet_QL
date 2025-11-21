package com.example.pfa.reservation.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class reservationUtils {
    private reservationUtils(){

    }

    public static ResponseEntity<String> getResponseEntity(String responseMessage, HttpStatus httpStatus){
        return new ResponseEntity<String>("{\"message\":\""+responseMessage+"\"}", httpStatus);
    }
}
