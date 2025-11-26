package com.example.pfa.reservation.wrapper;

import com.example.pfa.reservation.model.Reservation;
import com.example.pfa.reservation.model.ReservationStatus;
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
    private ReservationStatus statut; // Change to String if needed
    private Double montantTotal;

    private Long clientId;
    private String clientNom;

    private Long chambreId;
    private String chambreNumero;

    private Long hotelId;
    private String hotelNom;

    private List<ServiceWrapper> services;

    public ReservationWrapper(Reservation reservation) {
    }

    public static ReservationWrapper fromEntity(Reservation entity) {
        if (entity == null) return null;

        return ReservationWrapper.builder()
                .id(entity.getId())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .dateReservation(entity.getDateReservation())
                .statut(entity.getStatut()) // ou entity.getStatut().name() si tu veux un String
                .montantTotal(entity.getMontantTotal())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientNom(entity.getClient() != null ? entity.getClient().getNom() : null)
                .chambreId(entity.getChambre() != null ? entity.getChambre().getId() : null)
                .chambreNumero(entity.getChambre() != null ? entity.getChambre().getNumero() : null)
                .hotelId(entity.getHotel() != null ? entity.getHotel().getId() : null)
                .hotelNom(entity.getHotel() != null ? entity.getHotel().getNom() : null)
                .services(entity.getServices() != null ?
                        entity.getServices().stream()
                                .map(ServiceWrapper::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}
