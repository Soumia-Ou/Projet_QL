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
        if (reservation != null) {
            this.id = reservation.getId();
            this.dateDebut = reservation.getDateDebut();
            this.dateFin = reservation.getDateFin();
            this.dateReservation = reservation.getDateReservation();
            this.statut = reservation.getStatut();
            this.montantTotal = reservation.getMontantTotal();
            if (reservation.getClient() != null) {
                this.clientId = reservation.getClient().getId();
                this.clientNom = reservation.getClient().getNom();
            }
            if (reservation.getChambre() != null) {
                this.chambreId = reservation.getChambre().getId();
                this.chambreNumero = reservation.getChambre().getNumero();
            }
            if (reservation.getHotel() != null) {
                this.hotelId = reservation.getHotel().getId();
                this.hotelNom = reservation.getHotel().getNom();
            }
            this.services = reservation.getServices() != null ?
                    reservation.getServices().stream()
                            .map(ServiceWrapper::fromEntity)
                            .toList() : null; // Remplace collect(Collectors.toList())
        }
    }

    public static ReservationWrapper fromEntity(Reservation entity) {
        return new ReservationWrapper(entity);
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



}
