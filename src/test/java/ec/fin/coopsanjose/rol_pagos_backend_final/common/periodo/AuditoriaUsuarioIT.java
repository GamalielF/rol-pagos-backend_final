package ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional   // rollback: no deja datos en MIDAS_RHH
class AuditoriaUsuarioIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private PeriodoRepository periodoRepository;

    @AfterEach
    void limpiarSesion() {
        // Imprescindible: si la sesión queda en el hilo, PeriodoAuditoriaIT dejaría de ver "SISTEMA"
        SecurityContextHolder.clearContext();
    }

    @Test
    void creadoPorEsElUsuarioDeLaSesion() {
        UsuarioAutenticado usuario = new UsuarioAutenticado("fsepa", null, null, Set.of(Rol.RRHH));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority(Rol.RRHH.autoridad()))));

        Periodo periodo = new Periodo();
        periodo.setFechaPeriodo(LocalDate.of(2099, 2, 1));
        periodoRepository.save(periodo);
        em.flush();

        assertThat(periodo.getCreadoPor()).isEqualTo("fsepa");
        assertThat(periodo.getModificadoPor()).isEqualTo("fsepa");
    }
}
