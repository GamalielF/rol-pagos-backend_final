package ec.fin.coopsanjose.rol_pagos_backend_final.common.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.listener.RegistroEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

//clases abstracta

/** Columnas comunes de todas las tablas. AuditingEntityListener llena CREADO_* / MODIFICADO_*. */
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, RegistroEntityListener.class})
@Getter
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false)
    private Long id;

    @Setter
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // fuerza VARCHAR: evita que Hibernate espere un ENUM nativo de MySQL
    @Column(name = "ESTADO_REGISTRO", nullable = false, length = 10)
    private EstadoRegistro estadoRegistro = EstadoRegistro.ACTIVO;

    @CreatedBy
    @Column(name = "CREADO_POR", nullable = false, updatable = false, length = 100)
    private String creadoPor;

    @CreatedDate
    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @LastModifiedBy
    @Column(name = "MODIFICADO_POR", nullable = false, length = 100)
    private String modificadoPor;

    @LastModifiedDate
    @Column(name = "MODIFICADO_EN", nullable = false)
    private LocalDateTime modificadoEn;

    /** Bloqueo optimista: si dos usuarios editan el mismo registro, el segundo recibe un error en vez de pisar al primero. */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    public void inactivar() {
        this.estadoRegistro = EstadoRegistro.INACTIVO;
    }

    public void activar() {
        this.estadoRegistro = EstadoRegistro.ACTIVO;
    }
}