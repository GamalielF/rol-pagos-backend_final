CREATE TABLE ACCESOS_ROLES_PAGO (
                                    ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                                    TIPO_ACCESO        VARCHAR(20)  NOT NULL,
                                    RESULTADO          VARCHAR(10)  NOT NULL,
                                    DETALLE            VARCHAR(40)  NOT NULL,
                                    USERNAME_AD        VARCHAR(50)  NOT NULL,
                                    CEDULA_CONSULTADA  VARCHAR(10)  NULL,
                                    PERIODO            VARCHAR(7)   NULL,
                                    ROL_PAGO_ID        BIGINT       NULL,
                                    MOTIVO             VARCHAR(200) NULL,
                                    IP_ORIGEN          VARCHAR(45)  NULL,
                                    ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                                    CREADO_POR         VARCHAR(100) NOT NULL,
                                    CREADO_EN          DATETIME(6)  NOT NULL,
                                    MODIFICADO_POR     VARCHAR(100) NOT NULL,
                                    MODIFICADO_EN      DATETIME(6)  NOT NULL,
                                    VERSION            BIGINT       NOT NULL DEFAULT 0,
                                    CONSTRAINT PK_ACCESOS_ROLES_PAGO PRIMARY KEY (ID),
                                    CONSTRAINT CK_ACCESOS_ROLES_PAGO_TIPO CHECK (TIPO_ACCESO IN ('CONSULTA_PROPIA', 'CONSULTA_AJENA')),
                                    CONSTRAINT CK_ACCESOS_ROLES_PAGO_RESULTADO CHECK (RESULTADO IN ('PERMITIDO', 'DENEGADO')),
                                    CONSTRAINT CK_ACCESOS_ROLES_PAGO_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Bitacora de cada acceso a un rol de pago (permitido o denegado)';

CREATE INDEX IDX_ACCESOS_ROLES_PAGO_USUARIO ON ACCESOS_ROLES_PAGO (USERNAME_AD, CREADO_EN);
CREATE INDEX IDX_ACCESOS_ROLES_PAGO_RESULTADO ON ACCESOS_ROLES_PAGO (RESULTADO, CREADO_EN);