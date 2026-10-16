package ec.fin.coopsanjose.rol_pagos_backend_final.common.util;

/** Valida una cédula ecuatoriana con el algoritmo módulo 10. */
public final class CedulaValidator {

    private CedulaValidator() {
    }

    public static boolean esValida(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) {
            return false;
        }
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (!((provincia >= 1 && provincia <= 24) || provincia == 30)) {
            return false;
        }
        if (cedula.charAt(2) - '0' >= 6) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int digito = cedula.charAt(i) - '0';
            int producto = (i % 2 == 0) ? digito * 2 : digito;
            if (producto >= 10) {
                producto -= 9;
            }
            suma += producto;
        }
        return (10 - (suma % 10)) % 10 == (cedula.charAt(9) - '0');
    }
}