package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Un usuario del AD, identificado por su CÓDIGO DE USUARIO. La cédula es un atributo y puede repetirse. */
@Entity
@Table(name = "DIRECTORIO_AD",
        uniqueConstraints = @UniqueConstraint(name = "UK_DIRECTORIO_AD_USERNAME", columnNames = "USERNAME_AD"),
        indexes = @Index(name = "IDX_DIRECTORIO_AD_CEDULA", columnList = "CEDULA"))
@Getter
@Setter
@NoArgsConstructor
public class DirectorioAd extends BaseEntity {

    @Column(name = "USERNAME_AD", nullable = false, length = 50)
    private String usernameAd;

    @Column(name = "CEDULA", nullable = false, length = 10)
    private String cedula;

    @Column(name = "NOMBRE_COMPLETO", length = 150)
    private String nombreCompleto;

    @Column(name = "CARGO", length = 255)
    private String cargo;

    @Column(name = "EMAIL", length = 120)
    private String email;

    @Column(name = "SECUENCIA_OFICINA")
    private Integer secuenciaOficina;

    @Column(name = "FECHA_CREACION_AD")
    private LocalDate fechaCreacionAd;
}