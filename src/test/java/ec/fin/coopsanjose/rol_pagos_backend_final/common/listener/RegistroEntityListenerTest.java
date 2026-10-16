package ec.fin.coopsanjose.rol_pagos_backend_final.common.listener;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.OperacionNoPermitidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroEntityListenerTest {

    private final RegistroEntityListener listener = new RegistroEntityListener();

    @Test
    void noPermiteEliminar() {
        assertThatThrownBy(() -> listener.alEliminar(new Periodo()))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inactivar");
    }

    @Test
    void registroNuevoNaceActivo() {
        Periodo periodo = new Periodo();
        periodo.setEstadoRegistro(null);
        listener.alCrear(periodo);
        assertThat(periodo.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    @Test
    void inactivarYActivarCambianElEstado() {
        Periodo periodo = new Periodo();
        periodo.inactivar();
        assertThat(periodo.getEstadoRegistro()).isEqualTo(EstadoRegistro.INACTIVO);
        periodo.activar();
        assertThat(periodo.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
    }
}