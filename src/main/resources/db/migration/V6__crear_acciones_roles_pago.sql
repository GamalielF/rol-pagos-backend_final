CREATE TABLE ACCIONES_ROLES_PAGO (
                                     ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                                     TIPO_ACCION        VARCHAR(30)  NOT NULL,
                                     USERNAME_AD        VARCHAR(50)  NOT NULL,
                                     PERIODO            VARCHAR(7)   NULL,
                                     ROL_PAGO_ID        BIGINT       NULL,
                                     DETALLE            VARCHAR(500) NULL,
                                     IP_ORIGEN          VARCHAR(45)  NULL,
                                     ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                                     CREADO_POR         VARCHAR(100) NOT NULL,
                                     CREADO_EN          DATETIME(6)  NOT NULL,
                                     MODIFICADO_POR     VARCHAR(100) NOT NULL,
                                     MODIFICADO_EN      DATETIME(6)  NOT NULL,
                                     VERSION            BIGINT       NOT NULL DEFAULT 0,
                                     CONSTRAINT PK_ACCIONES_ROLES_PAGO PRIMARY KEY (ID),
                                     CONSTRAINT CK_ACCIONES_ROLES_PAGO_TIPO CHECK (TIPO_ACCION IN
                                                                                   ('CARGA_LOTE', 'ROL_REEMPLAZADO', 'ROL_INACTIVADO', 'ROL_ACTIVADO', 'PERIODO_PUBLICADO', 'REPORTE_EXPORTADO')),
                                     CONSTRAINT CK_ACCIONES_ROLES_PAGO_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Bitacora de las acciones de RRHH/Admin sobre los roles de pago';

CREATE INDEX IDX_ACCIONES_ROLES_PAGO_PERIODO ON ACCIONES_ROLES_PAGO (PERIODO, CREADO_EN);
CREATE INDEX IDX_ACCIONES_ROLES_PAGO_USUARIO ON ACCIONES_ROLES_PAGO (USERNAME_AD, CREADO_EN);

-- Para los reportes de accesos: por fecha y para la cobertura de lectura (por rol)
CREATE INDEX IDX_ACCESOS_ROLES_PAGO_FECHA ON ACCESOS_ROLES_PAGO (CREADO_EN);
CREATE INDEX IDX_ACCESOS_ROLES_PAGO_ROL ON ACCESOS_ROLES_PAGO (ROL_PAGO_ID);