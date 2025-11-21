package com.example.pfa.reservation.wrapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChambreWrapper {

    private Long id;
    private String numero;
    private String typeChambre;
    private Double prix;
    private Boolean disponibilite;
    private Long hotelId;

}
