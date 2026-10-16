package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VaultConfigIT {

    @Autowired
    private Environment entorno;

    @Test
    void lasCredencialesDeLaBaseDeDatosVienenDeVault() {
        assertThat(entorno.getProperty("db.username")).as("db.username solo existe en Vault").isNotBlank();
        assertThat(entorno.getProperty("db.password")).as("db.password solo existe en Vault").isNotBlank();
    }
}