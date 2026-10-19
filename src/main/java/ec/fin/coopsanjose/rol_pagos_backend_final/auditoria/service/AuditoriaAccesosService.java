package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccesoRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaAccesosService {

    public record Evento(TipoAcceso tipo, ResultadoAcceso resultado, DetalleAcceso detalle, String username,
                         String cedulaConsultada, String periodo, Long rolPagoId, String motivo, String ip) {
    }

    private final AccesoRolPagoRepository repositorio;

    /** Si esto falla, la excepción sube: no se entrega ningún rol sin dejar registro. */
    public void registrar(Evento evento) {
        AccesoRolPago acceso = new AccesoRolPago();
        acceso.setTipoAcceso(evento.tipo());
        acceso.setResultado(evento.resultado());
        acceso.setDetalle(evento.detalle());
        acceso.setUsernameAd(limitar(evento.username(), 50));
        acceso.setCedulaConsultada(limitar(evento.cedulaConsultada(), 10));
        acceso.setPeriodo(limitar(evento.periodo(), 7));
        acceso.setRolPagoId(evento.rolPagoId());
        acceso.setMotivo(limitar(evento.motivo(), 200));
        acceso.setIpOrigen(limitar(evento.ip(), 45));
        repositorio.save(acceso);

        if (evento.resultado() == ResultadoAcceso.DENEGADO) {
            log.warn("Acceso denegado a un rol de pago: usuario={}, tipo={}, detalle={}",
                    evento.username(), evento.tipo(), evento.detalle());
        }
    }

    private static String limitar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }
}