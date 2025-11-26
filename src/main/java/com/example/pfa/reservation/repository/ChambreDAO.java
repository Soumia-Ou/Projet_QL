package com.example.pfa.reservation.repository;

import com.example.pfa.reservation.model.Chambre;
import com.example.pfa.reservation.wrapper.ChambreWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChambreDAO extends JpaRepository<Chambre, Long> {

    @Query("SELECT new com.example.pfa.reservation.wrapper.ChambreWrapper(c.id, c.numero, c.typeChambre, c.prix, c.disponibilite, c.hotel.id) FROM Chambre c")
    List<ChambreWrapper> getAllChambres();

    @Query("SELECT new com.example.pfa.reservation.wrapper.ChambreWrapper(c.id, c.numero, c.typeChambre, c.prix, c.disponibilite, c.hotel.id) FROM Chambre c WHERE c.id = :id")
    Optional<ChambreWrapper> getChambreById(@Param("id") Long id);

    @Query("SELECT new com.example.pfa.reservation.wrapper.ChambreWrapper(c.id, c.numero, c.typeChambre, c.prix, c.disponibilite, c.hotel.id) FROM Chambre c WHERE c.hotel.id = :hotelId")
    List<ChambreWrapper> getChambresByHotelId(@Param("hotelId") Long hotelId);

    @Transactional
    @Modifying
    @Query("UPDATE Chambre c SET c.numero = :numero, c.typeChambre = :typeChambre, c.prix = :prix, c.disponibilite = :disponibilite WHERE c.id = :id")
    int updateChambre(
            @Param("numero") String numero,
            @Param("typeChambre") String typeChambre,
            @Param("prix") Double prix,
            @Param("disponibilite") Boolean disponibilite,
            @Param("id") Long id
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM Chambre c WHERE c.id = :id")
    void deleteChambre(@Param("id") Long id);

    @Query("SELECT new com.example.pfa.reservation.wrapper.ChambreWrapper(c.id, c.numero, c.typeChambre, c.prix, c.disponibilite, c.hotel.id) " +
            "FROM Chambre c " +
            "WHERE (:typeChambre IS NULL OR LOWER(c.typeChambre) LIKE LOWER(CONCAT('%', :typeChambre, '%'))) " +
            "AND (:prixMin IS NULL OR c.prix >= :prixMin) " +
            "AND (:prixMax IS NULL OR c.prix <= :prixMax) " +
            "AND (:disponibilite IS NULL OR c.disponibilite = :disponibilite)")
    List<ChambreWrapper> searchChambres(@Param("typeChambre") String typeChambre,
                                        @Param("prixMin") Double prixMin,
                                        @Param("prixMax") Double prixMax,
                                        @Param("disponibilite") Boolean disponibilite);

    @Query("SELECT new com.example.pfa.reservation.wrapper.ChambreWrapper(c.id, c.numero, c.typeChambre, c.prix, c.disponibilite, c.hotel.id) " +
            "FROM Chambre c " +
            "WHERE c.hotel.id = :hotelId AND c.disponibilite = true")
    List<ChambreWrapper> getAvailableChambresByHotelId(@Param("hotelId") Long hotelId);

}