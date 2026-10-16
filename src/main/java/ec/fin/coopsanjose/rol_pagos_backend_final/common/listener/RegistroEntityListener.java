package ec.fin.coopsanjose.rol_pagos_backend_final.common.listener;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.OperacionNoPermitidaException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

public class RegistroEntityListener {

    /** Todo registro nuevo nace ACTIVO, aunque alguien lo deje en null. */
    @PrePersist
    public void alCrear(BaseEntity entidad) {
        if (entidad.getEstadoRegistro() == null) {
            entidad.setEstadoRegistro(EstadoRegistro.ACTIVO);
        }
    }

    /** Capa 1 contra el borrado: cualquier remove() falla. */
    @PreRemove
    public void alEliminar(BaseEntity entidad) {
        throw new OperacionNoPermitidaException(
                "Los registros no se eliminan por razones de auditoría; use inactivar().");
    }
}
