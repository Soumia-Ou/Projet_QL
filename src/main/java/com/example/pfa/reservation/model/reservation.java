package com.example.pfa.reservation.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "reservation")
public class reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "date_reservation", nullable = false)
    private LocalDateTime dateReservation;

    @Column(name = "statut", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus statut;

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private user client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    private chambre chambre;

    @ManyToMany
    @JoinTable(
            name = "reservation_service",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<service> services;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private hotel hotel;


}
