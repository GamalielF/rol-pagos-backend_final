package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response;

import java.util.List;

public record CoberturaPeriodoDto(String periodo, long rolesPublicados, long leidos, long pendientes,
                                  List<String> nombresPendientes) {
}
