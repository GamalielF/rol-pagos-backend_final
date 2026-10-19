package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoArchivoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.PdfDePrueba;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.CargaRolesPagoService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage.AlmacenRolesPago;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Base de las pruebas de integración de los roles de pago. Cada prueba corre en una transacción que se revierte,
 * trabaja en un período lejano (2099-03) y guarda los archivos en una carpeta temporal: no toca datos reales.
 */
@SpringBootTest
@Transactional
public abstract class SoporteRolesPago {

    protected static final YearMonth PERIODO = YearMonth.of(2099, 3);
    protected static final String MES_PERIODO = "MARZO 2099";

    // Personas ficticias con cédulas que pasan el dígito verificador
    protected static final String CEDULA_JUAN = "1712345675";
    protected static final String NOMBRE_JUAN = "PEREZ LOPEZ JUAN CARLOS";
    protected static final String CEDULA_MARIA = "1709876542";
    protected static final String NOMBRE_MARIA = "GOMEZ RUIZ MARIA ELENA";

    /** Una sola carpeta temporal para toda la ejecución (los contextos de Spring se reutilizan entre clases). */
    private static final Path ALMACEN = crearAlmacenTemporal();

    @Autowired protected EntityManager em;
    @Autowired protected DirectorioAdRepository directorioRepository;
    @Autowired protected RolPagoRepository rolPagoRepository;
    @Autowired protected PeriodoRepository periodoRepository;
    @Autowired protected AlmacenRolesPago almacen;
    @Autowired protected CargaRolesPagoService carga;

    @DynamicPropertySource
    static void rutaDeAlmacenamiento(DynamicPropertyRegistry registro) {
        registro.add("app.roles.ruta-base", ALMACEN::toString);
    }

    @BeforeEach
    void limpiarDirectorio() {
        // Se revierte al terminar: así el directorio real no interfiere con las personas de la prueba
        em.createNativeQuery("DELETE FROM DIRECTORIO_AD").executeUpdate();
        em.flush();
        em.clear();
    }

    protected DirectorioAd persona(String usuario, String cedula, String nombre, boolean activo) {
        DirectorioAd d = new DirectorioAd();
        d.setUsernameAd(usuario);
        d.setCedula(cedula);
        d.setNombreCompleto(nombre);
        if (!activo) {
            d.inactivar();
        }
        return directorioRepository.save(d);
    }

    /** Un rol de pago de prueba: una página con el mes y el nombre del titular. */
    protected static byte[] rol(String mesAnio, String nombre) {
        return PdfDePrueba.crear("LIQUIDACION DE SUELDO", "MES AÑO", mesAnio, nombre);
    }

    protected static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivos", nombre, "application/pdf", contenido);
    }

    protected ResultadoArchivoDto cargarUno(String nombreArchivo, byte[] contenido) {
        return carga.cargar(PERIODO, List.of(archivo(nombreArchivo, contenido))).resultados().get(0);
    }

    /** El rol guardado para esa cédula en el período de prueba (lee de nuevo desde la base de datos). */
    protected RolPago rolDe(String cedula) {
        em.flush();
        em.clear();
        Long idPeriodo = periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow().getId();
        return rolPagoRepository.findByPeriodo_IdAndCedula(idPeriodo, cedula).orElseThrow();
    }

    private static Path crearAlmacenTemporal() {
        try {
            Path carpeta = Files.createTempDirectory("roles-pago-test-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> borrarRecursivo(carpeta)));
            return carpeta;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void borrarRecursivo(Path raiz) {
        try (Stream<Path> todo = Files.walk(raiz)) {
            todo.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignorado) {
            // limpieza de mejor esfuerzo
        }
    }
}