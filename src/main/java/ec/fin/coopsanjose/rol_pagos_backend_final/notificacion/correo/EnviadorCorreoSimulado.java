package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties.ResultadoSimulado;
import lombok.extern.slf4j.Slf4j;

/**
 * SOLO DESARROLLO (la configuración lo impide en producción). No envía nada: escribe el aviso en el log,
 * sin la dirección del destinatario, y permite simular fallos para probar los reintentos.
 */
@Slf4j
public class EnviadorCorreoSimulado implements EnviadorCorreo {

    private final ResultadoSimulado resultado;

    public EnviadorCorreoSimulado(ResultadoSimulado resultado) {
        this.resultado = resultado;
    }

    @Override
    public boolean estaConfigurado() {
        return true;
    }

    @Override
    public void enviar(String destinatario, String asunto, String texto, String referencia) {
        switch (resultado) {
            case OK -> log.info("[SIMULADO] Aviso {} | Asunto: {}\n{}\n(no se envió ningún correo real)", referencia, asunto, texto);
            case FALLO_TEMPORAL -> throw new EnvioCorreoException("Fallo simulado (temporal)", true, null);
            case FALLO_DEFINITIVO -> throw new EnvioCorreoException("Fallo simulado (definitivo)", false, null);
        }
    }
}
