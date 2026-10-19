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

/** Un acceso (permitido o denegado) a un rol de pago. Sin claves foráneas: es un historial. */
@Entity
@Table(name = "ACCESOS_ROLES_PAGO",
        indexes = {
                @Index(name = "IDX_ACCESOS_ROLES_PAGO_USUARIO", columnList = "USERNAME_AD, CREADO_EN"),
                @Index(name = "IDX_ACCESOS_ROLES_PAGO_RESULTADO", columnList = "RESULTADO, CREADO_EN")})
@Getter
@Setter
@NoArgsConstructor
public class AccesoRolPago extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "TIPO_ACCESO", nullable = false, length = 20)
    private TipoAcceso tipoAcceso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "RESULTADO", nullable = false, length = 10)
    private ResultadoAcceso resultado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "DETALLE", nullable = false, length = 40)
    private DetalleAcceso detalle;

    /** Quién consulta. */
    @Column(name = "USERNAME_AD", nullable = false, length = 50)
    private String usernameAd;

    /** La cédula tecleada (consulta propia) o la del titular (consulta de RRHH/Admin). */
    @Column(name = "CEDULA_CONSULTADA", length = 10)
    private String cedulaConsultada;

    @Column(name = "PERIODO", length = 7)
    private String periodo;

    @Column(name = "ROL_PAGO_ID")
    private Long rolPagoId;

    /** Motivo declarado por RRHH/Admin al consultar un rol ajeno. */
    @Column(name = "MOTIVO", length = 200)
    private String motivo;

    @Column(name = "IP_ORIGEN", length = 45)
    private String ipOrigen;
}