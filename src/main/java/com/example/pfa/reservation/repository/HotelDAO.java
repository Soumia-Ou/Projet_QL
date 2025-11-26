package com.example.pfa.reservation.repository;

import com.example.pfa.reservation.model.Hotel;

import com.example.pfa.reservation.wrapper.HotelWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelDAO extends JpaRepository<Hotel, Long> {

    @Query("SELECT new com.example.pfa.reservation.wrapper.HotelWrapper(h.id, h.nom, h.adresse, h.telephone, h.email, h.nombreEtoiles, h.image, h.adminHotelier.id) FROM Hotel h")
    List<HotelWrapper> getAllHotels();

    @Query("SELECT new com.example.pfa.reservation.wrapper.HotelWrapper(h.id, h.nom, h.adresse, h.telephone, h.email, h.nombreEtoiles, h.image, h.adminHotelier.id) FROM Hotel h WHERE h.id = :id")
    Optional<HotelWrapper> getHotelById(@Param("id") Long id);


    @Query("SELECT new com.example.pfa.reservation.wrapper.HotelWrapper(h.id, h.nom, h.adresse, h.telephone, h.email, h.nombreEtoiles, h.image, h.adminHotelier.id) FROM Hotel h WHERE h.adminHotelier.id = :adminId")
    List<HotelWrapper> getHotelsByAdminId(@Param("adminId") Long adminId);

    @Transactional
    @Modifying
    @Query("UPDATE Hotel h SET h.nom = :nom, h.adresse = :adresse, h.telephone = :telephone, h.email = :email, h.nombreEtoiles = :nombreEtoiles, h.image = :image WHERE h.id = :id")
    int updateHotel(
            @Param("nom") String nom,
            @Param("adresse") String adresse,
            @Param("telephone") String telephone,
            @Param("email") String email,
            @Param("nombreEtoiles") Integer nombreEtoiles,
            @Param("image") String image,
            @Param("id") Long id
    );

    List<Hotel> findByAdminHotelierId(Long adminId);

    @Query("SELECT new com.example.pfa.reservation.wrapper.HotelWrapper(h.id, h.nom, h.adresse, h.telephone, h.email, h.nombreEtoiles, h.image, h.adminHotelier.id) " +
            "FROM Hotel h " +
            "WHERE (:nom IS NULL OR LOWER(h.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) " +
            "AND (:adresse IS NULL OR LOWER(h.adresse) LIKE LOWER(CONCAT('%', :adresse, '%'))) " +
            "AND (:etoiles IS NULL OR h.nombreEtoiles = :etoiles)")
    List<HotelWrapper> searchHotels(@Param("nom") String nom,
                                    @Param("adresse") String adresse,
                                    @Param("etoiles") Integer etoiles);


}
