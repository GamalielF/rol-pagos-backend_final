package ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PERIODOS",
        uniqueConstraints = @UniqueConstraint(name = "UK_PERIODOS_FECHA_PERIODO", columnNames = "FECHA_PERIODO"))
@Getter
@Setter
@NoArgsConstructor
public class Periodo extends BaseEntity {

    /** Siempre el día 1 del mes (la BD lo garantiza con CK_PERIODOS_FECHA_DIA_UNO). */
    @Column(name = "FECHA_PERIODO", nullable = false)
    private LocalDate fechaPeriodo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ESTADO_PERIODO", nullable = false, length = 20)
    private EstadoPeriodo estadoPeriodo = EstadoPeriodo.CARGADO;

    @Column(name = "FECHA_PUBLICACION")
    private LocalDateTime fechaPublicacion;

    @Column(name = "PUBLICADO_POR", length = 100)
    private String publicadoPor;
}
