package com.example.pfa.reservation.jwt;

public class JwtTokenException extends RuntimeException {

    public JwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}