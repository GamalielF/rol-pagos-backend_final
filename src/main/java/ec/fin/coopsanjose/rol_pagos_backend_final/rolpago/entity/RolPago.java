package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * El rol de pago (PDF) de un titular en un período. El DUEÑO es el código de usuario; la cédula es solo
 * referencia. ESTADO_REGISTRO INACTIVO = retirado por RRHH (nunca se borra).
 */
@Entity
@Table(name = "ROLES_PAGO",
        uniqueConstraints = @UniqueConstraint(name = "UK_ROLES_PAGO_PERIODO_USUARIO",
                columnNames = {"PERIODO_ID", "USERNAME_AD"}),
        indexes = @Index(name = "IDX_ROLES_PAGO_USERNAME", columnList = "USERNAME_AD"))
@Getter
@Setter
@NoArgsConstructor
public class RolPago extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERIODO_ID", nullable = false)
    private Periodo periodo;

    @Column(name = "USERNAME_AD", nullable = false, length = 50)
    private String usernameAd;

    @Column(name = "CEDULA", nullable = false, length = 10)
    private String cedula;

    /** Ruta RELATIVA al almacenamiento: yyyy-MM/uuid.pdf */
    @Column(name = "RUTA_ARCHIVO", nullable = false, length = 255)
    private String rutaArchivo;

    @Column(name = "HASH_SHA256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "TAMANO_BYTES", nullable = false)
    private long tamanoBytes;

    /** Mes que declara el propio PDF (siempre día 1). */
    @Column(name = "MES_PDF", nullable = false)
    private LocalDate mesPdf;
}
