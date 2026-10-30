package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion;

import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository.NotificacionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service.DespachoNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service.NotificacionService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PublicacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.PublicacionService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Base de las pruebas de avisos. El enviador es un doble de prueba (nuestra propia interfaz) y los avisos
 * reales de la base de desarrollo se apartan dentro de la transacción de cada prueba, que se revierte.
 */
public abstract class SoporteNotificaciones extends SoporteRolesPago {

    protected static final String CEDULA_LUIS = "1723456784";
    protected static final String NOMBRE_LUIS = "SILVA TORRES LUIS ALBERTO";

    @MockitoBean protected DirectorioAdClient clienteAd;
    @MockitoBean protected EnviadorCorreo enviador;
    @Autowired protected NotificacionRolPagoRepository avisoRepository;
    @Autowired protected NotificacionService notificaciones;
    @Autowired protected DespachoNotificaciones despacho;
    @Autowired protected PublicacionService publicacion;
    @Autowired protected Clock clock;

    @BeforeEach
    void aislarYPrepararElEnviador() {
        em.createNativeQuery("UPDATE NOTIFICACIONES_ROLES_PAGO SET ESTADO = 'OMITIDA'").executeUpdate();
        em.flush();
        em.clear();
        when(enviador.estaConfigurado()).thenReturn(true);
    }

    protected DirectorioAd personaConCorreo(String usuario, String cedula, String nombre, LocalDate creacionEnAd) {
        DirectorioAd d = persona(usuario, cedula, nombre, true);
        d.setEmail(usuario + "@coopsanjose.fin.ec");
        d.setFechaCreacionAd(creacionEnAd);
        return directorioRepository.save(d);
    }

    /** Juan, María y Luis, con correo, y sus tres roles cargados (sin publicar). */
    protected void prepararTresPersonas() {
        personaConCorreo("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2023, 1, 1));
        personaConCorreo("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, LocalDate.of(2023, 1, 1));
        personaConCorreo("zz.luis", CEDULA_LUIS, NOMBRE_LUIS, LocalDate.of(2023, 1, 1));
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA)),
                archivo(CEDULA_LUIS + ".pdf", rol(MES_PERIODO, NOMBRE_LUIS))));
    }

    protected PublicacionDto publicar() {
        return publicacion.publicar(PERIODO, false, "zz.rrhh");
    }

    /** Los avisos del período de prueba, leídos de nuevo desde la base de datos. */
    protected List<NotificacionRolPago> avisos() {
        em.flush();
        em.clear();
        return periodoRepository.findByFechaPeriodo(PERIODO.atDay(1))
                .map(p -> avisoRepository.findByPeriodo_Id(p.getId()).stream()
                        .sorted(Comparator.comparing(NotificacionRolPago::getCedula)).toList())
                .orElse(List.of());
    }

    protected NotificacionRolPago avisoDe(String cedula) {
        return avisos().stream().filter(a -> a.getCedula().equals(cedula)).findFirst().orElseThrow();
    }
}
