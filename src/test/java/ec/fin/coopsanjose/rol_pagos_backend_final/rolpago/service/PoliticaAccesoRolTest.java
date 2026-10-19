package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaAccesoRolTest {

    private final PoliticaAccesoRol politica = new PoliticaAccesoRol();

    private static UsuarioAutenticado usuario(String cedula, Rol... roles) {
        return new UsuarioAutenticado("zz.prueba", cedula, null, Set.of(roles));
    }

    @Test
    void laCedulaPropiaEsAceptada() {
        assertThat(politica.evaluarConsultaPropia(usuario("1712345675", Rol.EMPLEADO), "1712345675"))
                .isEqualTo(DetalleAcceso.OK);
    }

    @Test
    void laCedulaDeOtraPersonaNoCoincide() {
        assertThat(politica.evaluarConsultaPropia(usuario("1712345675", Rol.EMPLEADO), "1709876542"))
                .isEqualTo(DetalleAcceso.CEDULA_NO_COINCIDE);
    }

    @Test
    void unaCedulaNulaNoCoincide() {
        assertThat(politica.evaluarConsultaPropia(usuario("1712345675", Rol.EMPLEADO), null))
                .isEqualTo(DetalleAcceso.CEDULA_NO_COINCIDE);
    }

    @Test
    void sinCedulaEnElDirectorioNoSePuedeConsultarNada() {
        assertThat(politica.evaluarConsultaPropia(usuario(null, Rol.EMPLEADO), "1712345675"))
                .isEqualTo(DetalleAcceso.SIN_CEDULA_EN_DIRECTORIO);
        assertThat(politica.evaluarConsultaPropia(usuario("  ", Rol.EMPLEADO), "1712345675"))
                .isEqualTo(DetalleAcceso.SIN_CEDULA_EN_DIRECTORIO);
    }

    @Test
    void sinUsuarioSeDeniega() {
        assertThat(politica.evaluarConsultaPropia(null, "1712345675"))
                .isEqualTo(DetalleAcceso.SIN_CEDULA_EN_DIRECTORIO);
    }

    @Test
    void rrhhYAdminPuedenConsultarRolesAjenos() {
        assertThat(politica.puedeConsultarAjeno(usuario(null, Rol.RRHH))).isTrue();
        assertThat(politica.puedeConsultarAjeno(usuario(null, Rol.ADMIN))).isTrue();
    }

    @Test
    void unEmpleadoNoPuedeConsultarRolesAjenos() {
        assertThat(politica.puedeConsultarAjeno(usuario("1712345675", Rol.EMPLEADO))).isFalse();
    }

    @Test
    void sinUsuarioNadiePuedeConsultarRolesAjenos() {
        assertThat(politica.puedeConsultarAjeno(null)).isFalse();
    }

    @Test
    void losRolesSeSuman() {
        assertThat(politica.puedeConsultarAjeno(usuario(null, Rol.EMPLEADO, Rol.RRHH))).isTrue();
    }
}
