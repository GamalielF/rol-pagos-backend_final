package ec.fin.coopsanjose.rol_pagos_backend_final.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PaginaDto<T>(List<T> elementos, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> PaginaDto<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaDto<>(pagina.getContent().stream().map(conversor).toList(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}