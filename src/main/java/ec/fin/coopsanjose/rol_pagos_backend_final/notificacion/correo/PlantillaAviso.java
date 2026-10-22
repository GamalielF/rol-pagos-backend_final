package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** El texto del aviso. No contiene datos del rol de pago ni nombres: solo el mes disponible. */
public final class PlantillaAviso {

    private static final Locale ES = Locale.forLanguageTag("es-EC");

    private PlantillaAviso() {
    }
    /**
     * Datos del aviso para la plantilla de la API. Incluye los nombres de la plantilla por defecto (FirstName, UserName,
     * CompanyName, NotificationDate, generado) y los propios (Mes, UrlSistema). No lleva datos del rol ni nombres de personas.
     */
    public static Map<String, String> variables(YearMonth periodo, String urlSistema, String emailDestino,
                                                long numeroAviso, LocalDate hoy) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("FirstName", "Colaborador/a");
        variables.put("UserName", emailDestino == null ? "" : emailDestino);
        variables.put("CompanyName", "COOPSANJOSE LTDA");
        variables.put("NotificationDate", hoy.toString());
        variables.put("generado", String.valueOf(numeroAviso));
        variables.put("Mes", mes(periodo));
        variables.put("UrlSistema", urlSistema == null ? "" : urlSistema.trim());
        return variables;
    }

    public static String asunto(YearMonth periodo) {
        return "Su rol de pago de " + mes(periodo) + " está disponible";
    }

    public static String cuerpo(YearMonth periodo, String urlSistema) {
        StringBuilder texto = new StringBuilder();
        texto.append("Estimado/a colaborador/a:\n\n");
        texto.append("Su rol de pago correspondiente a ").append(mes(periodo)).append(" ya está disponible.\n\n");
        if (urlSistema != null && !urlSistema.isBlank()) {
            texto.append("Para consultarlo, ingrese al sistema de Roles de Pago:\n").append(urlSistema.trim()).append("\n\n");
        }
        texto.append("Por su seguridad, este mensaje no contiene información de su rol de pago, ")
                .append("y nunca le solicitaremos su clave por correo.\n");
        texto.append("Es un mensaje automático: por favor no responda a esta dirección. ")
                .append("Si tiene dudas, comuníquese con Recursos Humanos.\n\n");
        texto.append("COOPSANJOSE LTDA\n");
        return texto.toString();
    }

    static String mes(YearMonth periodo) {
        return periodo.getMonth().getDisplayName(TextStyle.FULL, ES) + " de " + periodo.getYear();
    }
}
