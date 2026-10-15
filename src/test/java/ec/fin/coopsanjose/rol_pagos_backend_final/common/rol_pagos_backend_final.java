package ec.fin.coopsanjose.rol_pagos_backend_final.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ConvencionNombresTest {

    private static final String PAQUETE_BASE = "ec.fin.coopsanjose.rol_pagos_backend_final";
    private static final Pattern MAYUSCULAS = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    @Test
    void tablasYColumnasEstanEnMayusculas() throws ClassNotFoundException {
        List<Class<?>> entidades = buscarEntidades();
        assertThat(entidades).as("No se encontró ninguna @Entity: revise PAQUETE_BASE").isNotEmpty();

        List<String> errores = new ArrayList<>();
        for (Class<?> entidad : entidades) {
            Table tabla = entidad.getAnnotation(Table.class);
            if (tabla == null || !esValido(tabla.name())) {
                errores.add(entidad.getSimpleName() + ": @Table(name) ausente o fuera de convención");
            }
            // Incluye los campos heredados de @MappedSuperclass (BaseEntity)
            for (Class<?> c = entidad; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field campo : c.getDeclaredFields()) {
                    String problema = revisar(campo);
                    if (problema != null) {
                        errores.add(c.getSimpleName() + "." + campo.getName() + ": " + problema);
                    }
                }
            }
        }
        assertThat(errores).as("Violaciones de la convención de nombres").isEmpty();
    }

    private List<Class<?>> buscarEntidades() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<Class<?>> clases = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(PAQUETE_BASE)) {
            clases.add(Class.forName(bd.getBeanClassName()));
        }
        return clases;
    }

    private String revisar(Field campo) {
        int mod = campo.getModifiers();
        if (Modifier.isStatic(mod) || Modifier.isTransient(mod)
                || campo.isAnnotationPresent(Transient.class)
                || campo.isAnnotationPresent(OneToMany.class)
                || campo.isAnnotationPresent(ManyToMany.class)) {
            return null;
        }
        OneToOne unoAUno = campo.getAnnotation(OneToOne.class);
        if (campo.isAnnotationPresent(ManyToOne.class) || (unoAUno != null && unoAUno.mappedBy().isEmpty())) {
            JoinColumn jc = campo.getAnnotation(JoinColumn.class);
            return (jc != null && esValido(jc.name())) ? null : "@JoinColumn(name) ausente o fuera de convención";
        }
        if (unoAUno != null) {
            return null;   // lado inverso (mappedBy): no tiene columna propia
        }
        Column col = campo.getAnnotation(Column.class);
        return (col != null && esValido(col.name())) ? null : "@Column(name) ausente o fuera de convención";
    }

    private boolean esValido(String nombre) {
        return nombre != null && MAYUSCULAS.matcher(nombre).matches();
    }
}
