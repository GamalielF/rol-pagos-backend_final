package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.entity;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
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

/** Un rol elevado (ADMIN o RRHH) de un código de usuario. INACTIVO = revocado: nunca se borra. */
@Entity
@Table(name = "ASIGNACIONES_ROL",
        uniqueConstraints = @UniqueConstraint(name = "UK_ASIGNACIONES_ROL_USUARIO_ROL", columnNames = {"USERNAME_AD", "ROL"}))
@Getter
@Setter
@NoArgsConstructor
public class AsignacionRol extends BaseEntity {

    @Column(name = "USERNAME_AD", nullable = false, length = 50)
    private String usernameAd;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ROL", nullable = false, length = 10)
    private Rol rol;

    /** 1 = puede abrir los roles de pago de otras personas. Entero (y no boolean) para evitar ambigüedades con MySQL. */
    @Column(name = "VER_ROLES_AJENOS", nullable = false)
    private int verRolesAjenos;

    public boolean puedeVerRolesAjenos() {
        return verRolesAjenos == 1;
    }

    public void definirVerRolesAjenos(boolean valor) {
        this.verRolesAjenos = valor ? 1 : 0;
    }
}
