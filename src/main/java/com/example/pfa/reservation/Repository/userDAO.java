package com.example.pfa.reservation.Repository;

import com.example.pfa.reservation.model.user;
import com.example.pfa.reservation.wrapper.UserWrapper;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface userDAO extends JpaRepository <user , Long>{

    @Query("SELECT u FROM user u WHERE u.email = :email OR u.userName = :userName")
    user findByEmailOrUsername(@Param("email") String email,@Param("userName") String userName);

    @Query("SELECT new com.example.pfa.reservation.wrapper.UserWrapper(u.id, u.nom, u.prenom, u.userName, u.email, u.telephone) FROM user u WHERE u.role = 'CLIENT'")
    List<UserWrapper> getAllClient();

    @Query("SELECT new com.example.pfa.reservation.wrapper.UserWrapper(u.id, u.nom, u.prenom, u.userName, u.email, u.telephone) FROM user u WHERE u.role = 'HOTEL_ADMIN'")
    List<UserWrapper> getAllHotelAdmin();

    @Query("SELECT u.email FROM user u WHERE u.role = 'GLOBAL_ADMIN'")
    List<String> getAllGlobalAdmin();

    @Transactional
    @Modifying
    @Query("update user u set u.nom = :nom,u.prenom = :prenom ,u.email = :email ,u.userName = :userName ,u.telephone = :telephone where u.id = :id ")
    Integer updateProfil(@Param("nom") String nom,@Param("prenom") String prenom, @Param("email") String email ,@Param("userName") String username ,@Param("telephone") String telephone ,@Param("id") Long id);

    user findByEmail(String email);
    user findByUserName(String userName);
}
