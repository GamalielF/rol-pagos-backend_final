package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.PerfilDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Permiso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilServiceTest {

    private final DirectorioAdRepository repositorio = mock(DirectorioAdRepository.class);
    private final PerfilService servicio = new PerfilService(repositorio);

    private static UsuarioAutenticado usuario(String cedula, Set<Rol> roles, Set<Permiso> permisos) {
        return new UsuarioAutenticado("fsepa", cedula, "SEPA MORA FABIO", roles, permisos);
    }

    @Test
    void incluyeElCargoQueGuardaElDirectorio() {
        DirectorioAd fila = new DirectorioAd();
        fila.setCargo("ANALISTA DE TECNOLOGIA");
        when(repositorio.findByUsernameAd("fsepa")).thenReturn(Optional.of(fila));
        PerfilDto perfil = servicio.perfilDe(usuario("1712345675", Set.of(Rol.EMPLEADO), Set.of()));
        assertThat(perfil.cargo()).isEqualTo("ANALISTA DE TECNOLOGIA");
        assertThat(perfil.nombre()).isEqualTo("SEPA MORA FABIO");
    }

    @Test
    void sinFilaEnElDirectorioElCargoEsNulo() {
        when(repositorio.findByUsernameAd("fsepa")).thenReturn(Optional.empty());
        assertThat(servicio.perfilDe(usuario(null, Set.of(Rol.EMPLEADO), Set.of())).cargo()).isNull();
    }

    @Test
    void losRolesVanDelMasAlMenosPrivilegiadoSinImportarElOrdenDelConjunto() {
        when(repositorio.findByUsernameAd("fsepa")).thenReturn(Optional.empty());
        PerfilDto perfil = servicio.perfilDe(usuario(null, Set.of(Rol.EMPLEADO, Rol.RRHH, Rol.ADMIN), Set.of()));
        assertThat(perfil.roles()).containsExactly("ADMIN", "RRHH", "EMPLEADO");
    }

    @Test
    void laCedulaSaleEnmascaradaYSoloQuienLaTienePuedeConsultarSuRol() {
        when(repositorio.findByUsernameAd("fsepa")).thenReturn(Optional.empty());
        PerfilDto conCedula = servicio.perfilDe(usuario("1712345675", Set.of(Rol.EMPLEADO), Set.of()));
        assertThat(conCedula.cedulaEnmascarada()).isEqualTo("17******75").doesNotContain("1712345675");
        assertThat(conCedula.puedeConsultarSuRol()).isTrue();
        assertThat(servicio.perfilDe(usuario(null, Set.of(Rol.EMPLEADO), Set.of())).puedeConsultarSuRol()).isFalse();
    }

    @Test
    void losPermisosSeEntreganPorNombre() {
        when(repositorio.findByUsernameAd("fsepa")).thenReturn(Optional.empty());
        PerfilDto perfil = servicio.perfilDe(usuario(null, Set.of(Rol.RRHH), Set.of(Permiso.VER_ROLES_AJENOS)));
        assertThat(perfil.permisos()).containsExactly("VER_ROLES_AJENOS");
    }
}
