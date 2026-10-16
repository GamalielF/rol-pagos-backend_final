CREATE TABLE PERIODOS (
                          ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                          FECHA_PERIODO      DATE         NOT NULL,
                          ESTADO_PERIODO     VARCHAR(20)  NOT NULL DEFAULT 'CARGADO',
                          FECHA_PUBLICACION  DATETIME(6)  NULL,
                          PUBLICADO_POR      VARCHAR(100) NULL,
                          ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                          CREADO_POR         VARCHAR(100) NOT NULL,
                          CREADO_EN          DATETIME(6)  NOT NULL,
                          MODIFICADO_POR     VARCHAR(100) NOT NULL,
                          MODIFICADO_EN      DATETIME(6)  NOT NULL,
                          VERSION            BIGINT       NOT NULL DEFAULT 0,
                          CONSTRAINT PK_PERIODOS PRIMARY KEY (ID),
                          CONSTRAINT UK_PERIODOS_FECHA_PERIODO UNIQUE (FECHA_PERIODO),
                          CONSTRAINT CK_PERIODOS_FECHA_DIA_UNO CHECK (DAYOFMONTH(FECHA_PERIODO) = 1),
                          CONSTRAINT CK_PERIODOS_ESTADO_PERIODO CHECK (ESTADO_PERIODO IN ('CARGADO', 'PUBLICADO')),
                          CONSTRAINT CK_PERIODOS_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Periodos mensuales de roles de pago';