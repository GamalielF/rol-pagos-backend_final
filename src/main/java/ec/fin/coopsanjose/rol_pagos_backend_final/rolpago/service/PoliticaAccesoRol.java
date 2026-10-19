package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import org.springframework.stereotype.Component;

/** Única fuente de verdad sobre quién puede ver qué rol de pago. */
@Component
public class PoliticaAccesoRol {

    /**
     * Consulta propia: la cédula tecleada debe ser la de la sesión, que sale del directorio del AD.
     * Una cuenta sin cédula en el directorio (genérica, o inexistente) no puede consultar nada.
     */
    public DetalleAcceso evaluarConsultaPropia(UsuarioAutenticado usuario, String cedulaIngresada) {
        if (usuario == null || usuario.cedula() == null || usuario.cedula().isBlank()) {
            return DetalleAcceso.SIN_CEDULA_EN_DIRECTORIO;
        }
        if (!usuario.cedula().equals(cedulaIngresada)) {
            return DetalleAcceso.CEDULA_NO_COINCIDE;
        }
        return DetalleAcceso.OK;
    }

    /** Hoy: RRHH y Admin. En la Etapa 11 el Admin decidirá quién tiene este permiso. */
    public boolean puedeConsultarAjeno(UsuarioAutenticado usuario) {
        return usuario != null && (usuario.tieneRol(Rol.RRHH) || usuario.tieneRol(Rol.ADMIN));
    }
}