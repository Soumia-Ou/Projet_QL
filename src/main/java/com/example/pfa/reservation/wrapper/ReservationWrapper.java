package com.example.pfa.reservation.wrapper;


import com.example.pfa.reservation.model.ReservationStatus;
import com.example.pfa.reservation.model.reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationWrapper {
    private Long id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDateTime dateReservation; // Filled by backend on creation
    private ReservationStatus statut; // Change this to String
    private Double montantTotal;

    private Long clientId;
    private String clientNom;

    private Long chambreId;
    private String chambreNumero;

    private Long hotelId;
    private String hotelNom;

    private List<ServiceWrapper> services;

    public ReservationWrapper(Long id, LocalDate dateDebut, LocalDate dateFin, LocalDateTime dateReservation,
                              ReservationStatus statut, Double montantTotal,
                              Long clientId, String clientNom,
                              Long chambreId, String chambreNumero,
                              Long hotelId, String hotelNom) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.clientId = clientId;
        this.clientNom = clientNom;
        this.chambreId = chambreId;
        this.chambreNumero = chambreNumero;
        this.hotelId = hotelId;
        this.hotelNom = hotelNom;
    }
    public ReservationWrapper(Long id,
                              String clientNom,
                              String hotelNom,
                              LocalDate dateDebut,
                              LocalDate dateFin,
                              ReservationStatus statut,
                              Double montantTotal,
                              Long clientId,
                              Long chambreId,
                              String chambreNumero,
                              Long hotelId) {
        this.id = id;
        this.clientNom = clientNom;
        this.hotelNom = hotelNom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.clientId = clientId;
        this.chambreId = chambreId;
        this.chambreNumero = chambreNumero;
        this.hotelId = hotelId;
    }
    public static ReservationWrapper fromEntity(reservation entity) {
        return ReservationWrapper.builder()
                .id(entity.getId())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .dateReservation(entity.getDateReservation())
                .statut(entity.getStatut()) // Conversion en String si nécessaire
                .montantTotal(entity.getMontantTotal())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientNom(entity.getClient() != null ? entity.getClient().getNom() : null)
                .chambreId(entity.getChambre() != null ? entity.getChambre().getId() : null)
                .chambreNumero(entity.getChambre() != null ? entity.getChambre().getNumero() : null)
                .hotelId(entity.getHotel() != null ? entity.getHotel().getId() : null) // Correction ici
                .hotelNom(entity.getHotel() != null ? entity.getHotel().getNom() : null)
                .services(entity.getServices() != null ?
                        entity.getServices().stream()
                                .map(ServiceWrapper::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }


}
