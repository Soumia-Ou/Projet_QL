package com.example.pfa.reservation.repository;

import com.example.pfa.reservation.model.Service;
import com.example.pfa.reservation.wrapper.ServiceWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
@Repository
public interface ServiceDAO extends JpaRepository<Service, Long> {

    @Query("SELECT new com.example.pfa.reservation.wrapper.ServiceWrapper(s.id, s.nom, s.description, s.prix, s.hotel.id) FROM Service s")
    List<ServiceWrapper> getAllServices();

    @Query("SELECT new com.example.pfa.reservation.wrapper.ServiceWrapper(s.id, s.nom, s.description, s.prix, s.hotel.id) FROM Service s WHERE s.id = :id")
    Optional<ServiceWrapper> getServiceById(@Param("id") Long id);

    @Query("SELECT new com.example.pfa.reservation.wrapper.ServiceWrapper(s.id, s.nom, s.description, s.prix, s.hotel.id) FROM Service s WHERE s.hotel.id = :hotelId")
    List<ServiceWrapper> getServicesByHotelId(@Param("hotelId") Long hotelId);

    @Transactional
    @Modifying
    @Query("UPDATE Service s SET s.nom = :nom, s.description = :description, s.prix = :prix WHERE s.id = :id")
    int updateService(
            @Param("nom") String nom,
            @Param("description") String description,
            @Param("prix") Double prix,
            @Param("id") Long id
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM Service s WHERE s.id = :id")
    void deleteService(@Param("id") Long id);

    @Query("SELECT new com.example.pfa.reservation.wrapper.ServiceWrapper(s.id, s.nom, s.description, s.prix, s.hotel.id) " +
            "FROM Service s " +
            "WHERE (:nom IS NULL OR LOWER(s.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) " +
            "AND (:prixMin IS NULL OR s.prix >= :prixMin) " +
            "AND (:prixMax IS NULL OR s.prix <= :prixMax)")
    List<ServiceWrapper> searchServices(@Param("nom") String nom,
                                        @Param("prixMin") Double prixMin,
                                        @Param("prixMax") Double prixMax);


}
