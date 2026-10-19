package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una acción de RRHH/Admin sobre los roles de pago. Sin claves foráneas: es un historial. */
@Entity
@Table(name = "ACCIONES_ROLES_PAGO",
        indexes = {
                @Index(name = "IDX_ACCIONES_ROLES_PAGO_PERIODO", columnList = "PERIODO, CREADO_EN"),
                @Index(name = "IDX_ACCIONES_ROLES_PAGO_USUARIO", columnList = "USERNAME_AD, CREADO_EN")})
@Getter
@Setter
@NoArgsConstructor
public class AccionRolPago extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "TIPO_ACCION", nullable = false, length = 30)
    private TipoAccion tipoAccion;

    @Column(name = "USERNAME_AD", nullable = false, length = 50)
    private String usernameAd;

    @Column(name = "PERIODO", length = 7)
    private String periodo;

    @Column(name = "ROL_PAGO_ID")
    private Long rolPagoId;

    @Column(name = "DETALLE", length = 500)
    private String detalle;

    @Column(name = "IP_ORIGEN", length = 45)
    private String ipOrigen;
}
