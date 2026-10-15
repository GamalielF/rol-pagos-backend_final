package ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional   // cada prueba hace rollback: no deja datos en MIDAS_RHH
class PeriodoAuditoriaIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private PeriodoRepository periodoRepository;

    private Periodo nuevoPeriodo() {
        Periodo periodo = new Periodo();
        periodo.setFechaPeriodo(LocalDate.of(2099, 1, 1));
        return periodo;
    }

    @Test
    void alCrearSeLlenanLosCamposDeAuditoria() {
        Periodo periodo = periodoRepository.save(nuevoPeriodo());
        em.flush();

        assertThat(periodo.getId()).isNotNull();
        assertThat(periodo.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(periodo.getCreadoPor()).isEqualTo("SISTEMA");
        assertThat(periodo.getCreadoEn()).isNotNull();
        assertThat(periodo.getModificadoPor()).isEqualTo("SISTEMA");
        assertThat(periodo.getVersion()).isZero();
    }

    @Test
    void alModificarSubeLaVersionYSeActualizaModificadoEn() {
        Periodo periodo = periodoRepository.save(nuevoPeriodo());
        em.flush();

        periodo.setEstadoPeriodo(EstadoPeriodo.PUBLICADO);
        em.flush();

        assertThat(periodo.getVersion()).isEqualTo(1L);
        assertThat(periodo.getModificadoEn()).isAfterOrEqualTo(periodo.getCreadoEn());
    }

    @Test
    void noSePuedeEliminarFisicamente() {
        Periodo periodo = periodoRepository.save(nuevoPeriodo());
        em.flush();

        assertThatThrownBy(() -> em.remove(periodo)).hasMessageContaining("inactivar");
    }
}