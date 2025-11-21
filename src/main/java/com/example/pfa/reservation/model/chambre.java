package com.example.pfa.reservation.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "chambre")
public class chambre implements Serializable{

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false)
    private String numero;

    @Column(name = "type_chambre", nullable = false)
    private String typeChambre;

    @Column(name = "prix", nullable = false)
    private Double prix;

    @Column(name = "disponibilite", nullable = false)
    private Boolean disponibilite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private hotel hotel;


}

