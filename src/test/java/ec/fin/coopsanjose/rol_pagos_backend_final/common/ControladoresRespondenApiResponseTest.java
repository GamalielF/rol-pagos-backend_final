package ec.fin.coopsanjose.rol_pagos_backend_final.common;

import ec.fin.coopsanjose.rol_pagos_backend_final.RolPagosBackendFinalApplication;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Regla del proyecto: todo endpoint responde con ApiResponse (o ResponseEntity<ApiResponse<...>>). */
class ControladoresRespondenApiResponseTest {

    private static final String PAQUETE_BASE = RolPagosBackendFinalApplication.class.getPackageName();

    @Test
    void todosLosEndpointsDevuelvenApiResponse() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> errores = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(PAQUETE_BASE)) {
            Class<?> controlador = Class.forName(bd.getBeanClassName());
            for (Method metodo : controlador.getDeclaredMethods()) {
                boolean esEndpoint = AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class);
                if (esEndpoint && !devuelveApiResponse(metodo.getGenericReturnType())) {
                    errores.add(controlador.getSimpleName() + "." + metodo.getName()
                            + " devuelve " + metodo.getGenericReturnType().getTypeName());
                }
            }
        }
        assertThat(errores).as("Endpoints que no devuelven ApiResponse").isEmpty();
    }

    private boolean devuelveApiResponse(Type tipo) {
        if (tipo instanceof Class<?> clase) {
            return ApiResponse.class.equals(clase);
        }
        if (tipo instanceof ParameterizedType parametrizado) {
            Type base = parametrizado.getRawType();
            if (ApiResponse.class.equals(base)) {
                return true;
            }
            if (ResponseEntity.class.equals(base)) {
                return devuelveApiResponse(parametrizado.getActualTypeArguments()[0]);
            }
        }
        return false;
    }
}
