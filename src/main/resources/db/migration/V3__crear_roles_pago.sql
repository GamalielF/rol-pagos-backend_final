CREATE TABLE ROLES_PAGO (
                            ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                            PERIODO_ID         BIGINT       NOT NULL,
                            USERNAME_AD        VARCHAR(50)  NOT NULL,
                            CEDULA             VARCHAR(10)  NOT NULL,
                            RUTA_ARCHIVO       VARCHAR(255) NOT NULL,
                            HASH_SHA256        VARCHAR(64)  NOT NULL,
                            TAMANO_BYTES       BIGINT       NOT NULL,
                            MES_PDF            DATE         NOT NULL,
                            ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                            CREADO_POR         VARCHAR(100) NOT NULL,
                            CREADO_EN          DATETIME(6)  NOT NULL,
                            MODIFICADO_POR     VARCHAR(100) NOT NULL,
                            MODIFICADO_EN      DATETIME(6)  NOT NULL,
                            VERSION            BIGINT       NOT NULL DEFAULT 0,
                            CONSTRAINT PK_ROLES_PAGO PRIMARY KEY (ID),
                            CONSTRAINT UK_ROLES_PAGO_PERIODO_USUARIO UNIQUE (PERIODO_ID, USERNAME_AD),
                            CONSTRAINT FK_ROLES_PAGO_PERIODO FOREIGN KEY (PERIODO_ID) REFERENCES PERIODOS (ID),
                            CONSTRAINT FK_ROLES_PAGO_DIRECTORIO FOREIGN KEY (USERNAME_AD) REFERENCES DIRECTORIO_AD (USERNAME_AD),
                            CONSTRAINT CK_ROLES_PAGO_MES_DIA_UNO CHECK (DAYOFMONTH(MES_PDF) = 1),
                            CONSTRAINT CK_ROLES_PAGO_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Rol de pago (PDF) de un titular en un periodo';

CREATE INDEX IDX_ROLES_PAGO_USERNAME ON ROLES_PAGO (USERNAME_AD);