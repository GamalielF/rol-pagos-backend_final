package ec.fin.coopsanjose.rol_pagos_backend_final;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "dateTimeProvider")
public class RolPagosBackendFinalApplication {

	public static void main(String[] args) {
		SpringApplication.run(RolPagosBackendFinalApplication.class, args);
	}
}
