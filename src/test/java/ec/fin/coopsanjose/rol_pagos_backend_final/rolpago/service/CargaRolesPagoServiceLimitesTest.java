package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccionesService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config.RolPagoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.LectorPdfRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage.AlmacenRolesPago;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.unit.DataSize;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Los topes se validan ANTES de tocar la base de datos o el disco. */
class CargaRolesPagoServiceLimitesTest {

    private static final YearMonth PERIODO = YearMonth.of(2099, 3);

    private CargaRolesPagoService servicio(int maximoArchivos) {
        RolPagoProperties propiedades = new RolPagoProperties("./data/no-se-usa", DataSize.ofMegabytes(5), maximoArchivos, 1);
        return new CargaRolesPagoService(mock(RolPagoRepository.class), mock(PeriodoRepository.class),
                mock(ResolutorTitular.class), new LectorPdfRolPago(), mock(AlmacenRolesPago.class),
                propiedades, mock(PlatformTransactionManager.class), mock(AuditoriaAccionesService.class));
    }

    private static MockMultipartFile archivo(String nombre) {
        return new MockMultipartFile("archivos", nombre, "application/pdf", new byte[]{1});
    }

    @Test
    void sinArchivosSeRechaza() {
        assertThatThrownBy(() -> servicio(500).cargar(PERIODO, List.of()))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("al menos un archivo");
    }

    @Test
    void unaListaNulaSeRechaza() {
        assertThatThrownBy(() -> servicio(500).cargar(PERIODO, null))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void masArchivosQueElMaximoSeRechazanTodos() {
        List<MockMultipartFile> tres = List.of(archivo("a.pdf"), archivo("b.pdf"), archivo("c.pdf"));

        assertThatThrownBy(() -> servicio(2).cargar(PERIODO, List.copyOf(tres)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("Máximo 2 archivos");
    }
}
