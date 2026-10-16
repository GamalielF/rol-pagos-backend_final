package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.mapper;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.UsuarioAdDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.PerfilDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DirectorioAdMapperTest {

    private final DirectorioAdMapper mapper = new DirectorioAdMapper(propiedades("coopsanjose.fin.ec"));

    private static AdProperties propiedades(String dominio) {
        return new AdProperties("http://ad.test", "", null, Duration.ofSeconds(5), Duration.ofSeconds(30),
                dominio, List.of(), new AdProperties.Sincronizacion(false, "0 0 3 * * *", 50));
    }

    private static UsuarioAdDto dto(String usuario, String identificacion, Integer estatus) {
        return new UsuarioAdDto(usuario, "  PEREZ   LOPEZ JUAN ", identificacion, " ANALISTA   DE CREDITO ",
                1, "2023-04-14", estatus);
    }

    @Test
    void colapsaEspaciosYGuardaElCodigoEnMinusculas() {
        FilaDirectorio fila = mapper.aFila(dto("  JPerez ", "1712345675", 1));

        assertThat(fila.username()).isEqualTo("jperez");
        assertThat(fila.nombre()).isEqualTo("PEREZ LOPEZ JUAN");
        assertThat(fila.cargo()).isEqualTo("ANALISTA DE CREDITO");
        assertThat(fila.fechaCreacion()).isEqualTo(LocalDate.of(2023, 4, 14));
    }

    @Test
    void unaCedulaDeNueveDigitosRecuperaElCeroInicial() {
        assertThat(mapper.aFila(dto("jperez", "123456789", 1)).cedula()).isEqualTo("0123456789");
    }

    @Test
    void descartaLosRegistrosSinCodigoOSinIdentificacionValida() {
        assertThat(mapper.aFila(dto("jperez", "0", 1))).isNull();
        assertThat(mapper.aFila(dto("jperez", null, 1))).isNull();
        assertThat(mapper.aFila(dto("   ", "1712345675", 1))).isNull();
    }

    @Test
    void soloElEstatusUnoEsActivo() {
        assertThat(mapper.aFila(dto("jperez", "1712345675", 1)).activo()).isTrue();
        assertThat(mapper.aFila(dto("jperez", "1712345675", 0)).activo()).isFalse();
        assertThat(mapper.aFila(dto("jperez", "1712345675", null)).activo()).isFalse();
    }

    @Test
    void elCorreoSeArmaConElCodigoDeUsuario() {
        DirectorioAd d = new DirectorioAd();
        mapper.copiar(d, mapper.aFila(dto("jperez", "1712345675", 1)));

        assertThat(d.getEmail()).isEqualTo("jperez@coopsanjose.fin.ec");
    }

    @Test
    void sinDominioConfiguradoNoSeInventaUnCorreo() {
        DirectorioAdMapper sinDominio = new DirectorioAdMapper(propiedades(""));
        DirectorioAd d = new DirectorioAd();
        sinDominio.copiar(d, sinDominio.aFila(dto("jperez", "1712345675", 1)));

        assertThat(d.getEmail()).isNull();
    }

    @Test
    void detectaCambiosYNoInventaDiferencias() {
        FilaDirectorio fila = mapper.aFila(dto("jperez", "1712345675", 1));
        DirectorioAd d = new DirectorioAd();
        d.setUsernameAd("jperez");
        mapper.copiar(d, fila);

        assertThat(mapper.difiere(d, fila)).isFalse();

        d.setCargo("OTRO CARGO");
        assertThat(mapper.difiere(d, fila)).isTrue();

        mapper.copiar(d, fila);
        d.inactivar();
        assertThat(mapper.difiere(d, fila)).isTrue();
        assertThat(d.getEstadoRegistro()).isEqualTo(EstadoRegistro.INACTIVO);
    }

    @Test
    void elPerfilNuncaExponeLaCedulaCompleta() {
        DirectorioAd d = new DirectorioAd();
        d.setUsernameAd("jperez");
        d.setCedula("1712345675");

        PerfilDirectorioDto perfil = mapper.aPerfil(d);

        assertThat(perfil.cedulaEnmascarada()).isEqualTo("17******75");
        assertThat(perfil.toString()).doesNotContain("1712345675");
    }
}