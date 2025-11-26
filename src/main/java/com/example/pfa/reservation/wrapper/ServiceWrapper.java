package com.example.pfa.reservation.wrapper;

import com.example.pfa.reservation.model.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceWrapper {

    private Long id;
    private String nom;
    private String description;
    private Double prix;
    private Long hotelId;

    public static ServiceWrapper fromEntity(Service entity) {
        return ServiceWrapper.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .prix(entity.getPrix())
                .hotelId(entity.getHotel() != null ? entity.getHotel().getId() : null)
                .build();
    }

}

