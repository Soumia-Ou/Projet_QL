package com.example.pfa.reservation.wrapper;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class UserWrapper {


    private Long id;

    private String nom;

    private String prenom;

    private String email;

    private String userName;

    private String telephone;

    public UserWrapper(Long id, String nom, String prenom, String userName, String email,String telephone) {
        this.telephone = telephone;
        this.userName = userName;
        this.prenom = prenom;
        this.nom = nom;
        this.id = id;
        this.email = email;
    }
}
