package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Una corrida de sincronización. CREADO_POR dice quién la lanzó (usuario o SISTEMA). */
@Entity
@Table(name = "SINCRONIZACIONES_AD")
@Getter
@Setter
@NoArgsConstructor
public class SincronizacionAd extends BaseEntity {

    @Column(name = "INICIO", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "FIN")
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "RESULTADO", nullable = false, length = 10)
    private ResultadoSincronizacion resultado;

    @Column(name = "RECIBIDOS", nullable = false)
    private int recibidos;

    @Column(name = "CREADOS", nullable = false)
    private int creados;

    @Column(name = "ACTUALIZADOS", nullable = false)
    private int actualizados;

    @Column(name = "SIN_CAMBIOS", nullable = false)
    private int sinCambios;

    @Column(name = "INACTIVADOS", nullable = false)
    private int inactivados;

    @Column(name = "OMITIDOS", nullable = false)
    private int omitidos;

    @Column(name = "MENSAJE", length = 500)
    private String mensaje;
}