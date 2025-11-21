package com.example.pfa.reservation.wrapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class HotelWrapper {
    private Long id;
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private Integer nombreEtoiles;
    private String image;
    private Long adminId;

}
