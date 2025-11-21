package com.example.pfa.reservation.Repository;

import com.example.pfa.reservation.model.ReservationStatus;
import com.example.pfa.reservation.model.reservation;
import com.example.pfa.reservation.wrapper.ReservationWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface reservationDAO extends JpaRepository<reservation, Long> {

    @Query("SELECT new com.example.pfa.reservation.wrapper.ReservationWrapper(" +
            "r.id, r.dateDebut, r.dateFin, r.dateReservation, " +
            "r.statut, r.montantTotal, " +
            "r.client.id, r.client.nom, " +
            "r.chambre.id, r.chambre.numero, " +
            "r.hotel.id, r.hotel.nom" +
            ") FROM reservation r WHERE r.client.id = :clientId")
    List<ReservationWrapper> findReservationsByClientId(@Param("clientId") Long clientId);

    @Transactional
    @Modifying
    @Query("UPDATE reservation r SET r.statut = 'CONFIRMED' WHERE r.id = :id")
    int confirmerReservation(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE reservation r SET r.statut = 'CANCELLED' WHERE r.id = :id")
    int annulerReservation(@Param("id") Long id);

    // Calculer le prix total (chambre + services) via JPQL avec jointures
    @Query("SELECT (r.chambre.prix + COALESCE(SUM(s.prix), 0)) FROM reservation r LEFT JOIN r.services s WHERE r.id = :id GROUP BY r.id, r.chambre.prix")
    Double calculerPrixTotalReservation(@Param("id") Long id);

    // Calculer le montant total de toutes les réservations d'un client
    @Query("SELECT COALESCE(SUM(r.montantTotal), 0) FROM reservation r WHERE r.client.id = :clientId")
    Double getTotalAmountByClientId(@Param("clientId") Long clientId);

    // Récupérer toutes les réservations d'un hôtel
    @Query("SELECT r FROM reservation r WHERE r.hotel.id = :hotelId")
    List<reservation> findReservationsByHotelId(@Param("hotelId") Long hotelId);

    // Récupérer toutes les réservations en cours (aujourd'hui inclus)
    @Query("SELECT r FROM reservation r WHERE r.dateDebut <= CURRENT_DATE AND r.dateFin >= CURRENT_DATE AND r.statut = :statut")
    List<reservation> findActiveReservations(@Param("statut") ReservationStatus statut);

    // Mettre à jour le statut d'une réservation
    @Transactional
    @Modifying
    @Query("UPDATE reservation r SET r.statut = :statut WHERE r.id = :id")
    int updateReservationStatus(@Param("id") Long id, @Param("statut") ReservationStatus statut);

    // Supprimer une réservation par ID
    @Transactional
    @Modifying
    @Query("DELETE FROM reservation r WHERE r.id = :id")
    void deleteReservation(@Param("id") Long id);

    @Query("SELECT new com.example.pfa.reservation.wrapper.ReservationWrapper(" +
            "r.id, c.nom, h.nom, r.dateDebut, r.dateFin, r.statut, r.montantTotal, " +
            "c.id, ch.id, ch.numero, h.id) " +
            "FROM reservation r " +
            "JOIN r.client c " +
            "JOIN r.hotel h " +
            "JOIN r.chambre ch " +
            "WHERE (:clientId IS NULL OR r.client.id = :clientId) AND " +
            "(:hotelId IS NULL OR r.hotel.id = :hotelId) AND " +
            "(:dateDebut IS NULL OR r.dateDebut >= :dateDebut) AND " +
            "(:dateFin IS NULL OR r.dateFin <= :dateFin) AND " +
            "(:statut IS NULL OR UPPER(r.statut) = UPPER(:statut))")
    List<ReservationWrapper> searchReservations(@Param("clientId") Long clientId,
                                                         @Param("hotelId") Long hotelId,
                                                         @Param("dateDebut") LocalDate dateDebut,
                                                         @Param("dateFin") LocalDate dateFin,
                                                         @Param("statut") String statut);
}
