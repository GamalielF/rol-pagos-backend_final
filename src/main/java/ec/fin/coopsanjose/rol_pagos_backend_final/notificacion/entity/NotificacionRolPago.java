package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Un aviso por persona (cédula) y período. */
@Entity
@Table(name = "NOTIFICACIONES_ROLES_PAGO",
        uniqueConstraints = @UniqueConstraint(name = "UK_NOTIFICACIONES_PERIODO_CEDULA",
                columnNames = {"PERIODO_ID", "CEDULA"}),
        indexes = @Index(name = "IDX_NOTIFICACIONES_DESPACHO", columnList = "ESTADO, PROXIMO_INTENTO"))
@Getter
@Setter
@NoArgsConstructor
public class NotificacionRolPago extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERIODO_ID", nullable = false)
    private Periodo periodo;

    @Column(name = "ROL_PAGO_ID", nullable = false)
    private Long rolPagoId;

    @Column(name = "CEDULA", nullable = false, length = 10)
    private String cedula;

    /** Cuenta elegida al encolar: la activa de creación más reciente. */
    @Column(name = "USERNAME_DESTINO", length = 50)
    private String usernameDestino;

    @Column(name = "EMAIL_DESTINO", length = 120)
    private String emailDestino;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ESTADO", nullable = false, length = 10)
    private EstadoNotificacion estado;

    @Column(name = "INTENTOS", nullable = false)
    private int intentos;

    @Column(name = "PROXIMO_INTENTO")
    private LocalDateTime proximoIntento;

    @Column(name = "ULTIMO_INTENTO")
    private LocalDateTime ultimoIntento;

    @Column(name = "ENVIADA_EN")
    private LocalDateTime enviadaEn;

    /** Categoría del fallo, sin detalles técnicos ni datos personales. */
    @Column(name = "ULTIMO_ERROR", length = 200)
    private String ultimoError;
}
