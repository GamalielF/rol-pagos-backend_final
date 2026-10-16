CREATE TABLE DIRECTORIO_AD (
                               ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                               USERNAME_AD        VARCHAR(50)  NOT NULL,
                               CEDULA             VARCHAR(10)  NOT NULL,
                               NOMBRE_COMPLETO    VARCHAR(150) NULL,
                               CARGO              VARCHAR(255) NULL,
                               EMAIL              VARCHAR(120) NULL,
                               SECUENCIA_OFICINA  INT          NULL,
                               FECHA_CREACION_AD  DATE         NULL,
                               ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                               CREADO_POR         VARCHAR(100) NOT NULL,
                               CREADO_EN          DATETIME(6)  NOT NULL,
                               MODIFICADO_POR     VARCHAR(100) NOT NULL,
                               MODIFICADO_EN      DATETIME(6)  NOT NULL,
                               VERSION            BIGINT       NOT NULL DEFAULT 0,
                               CONSTRAINT PK_DIRECTORIO_AD PRIMARY KEY (ID),
                               CONSTRAINT UK_DIRECTORIO_AD_USERNAME UNIQUE (USERNAME_AD),
                               CONSTRAINT CK_DIRECTORIO_AD_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Copia local del directorio del AD (un registro por codigo de usuario)';

CREATE INDEX IDX_DIRECTORIO_AD_CEDULA ON DIRECTORIO_AD (CEDULA);

CREATE TABLE SINCRONIZACIONES_AD (
                                     ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                                     INICIO             DATETIME(6)  NOT NULL,
                                     FIN                DATETIME(6)  NULL,
                                     RESULTADO          VARCHAR(10)  NOT NULL,
                                     RECIBIDOS          INT          NOT NULL DEFAULT 0,
                                     CREADOS            INT          NOT NULL DEFAULT 0,
                                     ACTUALIZADOS       INT          NOT NULL DEFAULT 0,
                                     SIN_CAMBIOS        INT          NOT NULL DEFAULT 0,
                                     INACTIVADOS        INT          NOT NULL DEFAULT 0,
                                     OMITIDOS           INT          NOT NULL DEFAULT 0,
                                     MENSAJE            VARCHAR(500) NULL,
                                     ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                                     CREADO_POR         VARCHAR(100) NOT NULL,
                                     CREADO_EN          DATETIME(6)  NOT NULL,
                                     MODIFICADO_POR     VARCHAR(100) NOT NULL,
                                     MODIFICADO_EN      DATETIME(6)  NOT NULL,
                                     VERSION            BIGINT       NOT NULL DEFAULT 0,
                                     CONSTRAINT PK_SINCRONIZACIONES_AD PRIMARY KEY (ID),
                                     CONSTRAINT CK_SINCRONIZACIONES_AD_RESULTADO CHECK (RESULTADO IN ('EXITOSA', 'FALLIDA')),
                                     CONSTRAINT CK_SINCRONIZACIONES_AD_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Bitacora de cada sincronizacion con el AD';