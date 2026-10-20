CREATE TABLE NOTIFICACIONES_ROLES_PAGO (
                                           ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                                           PERIODO_ID         BIGINT       NOT NULL,
                                           ROL_PAGO_ID        BIGINT       NOT NULL,
                                           CEDULA             VARCHAR(10)  NOT NULL,
                                           USERNAME_DESTINO   VARCHAR(50)  NULL,
                                           EMAIL_DESTINO      VARCHAR(120) NULL,
                                           ESTADO             VARCHAR(10)  NOT NULL,
                                           INTENTOS           INT          NOT NULL DEFAULT 0,
                                           PROXIMO_INTENTO    DATETIME(6)  NULL,
                                           ULTIMO_INTENTO     DATETIME(6)  NULL,
                                           ENVIADA_EN         DATETIME(6)  NULL,
                                           ULTIMO_ERROR       VARCHAR(200) NULL,
                                           ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                                           CREADO_POR         VARCHAR(100) NOT NULL,
                                           CREADO_EN          DATETIME(6)  NOT NULL,
                                           MODIFICADO_POR     VARCHAR(100) NOT NULL,
                                           MODIFICADO_EN      DATETIME(6)  NOT NULL,
                                           VERSION            BIGINT       NOT NULL DEFAULT 0,
                                           CONSTRAINT PK_NOTIFICACIONES_ROLES_PAGO PRIMARY KEY (ID),
                                           CONSTRAINT UK_NOTIFICACIONES_PERIODO_CEDULA UNIQUE (PERIODO_ID, CEDULA),
                                           CONSTRAINT FK_NOTIFICACIONES_PERIODO FOREIGN KEY (PERIODO_ID) REFERENCES PERIODOS (ID),
                                           CONSTRAINT FK_NOTIFICACIONES_ROL_PAGO FOREIGN KEY (ROL_PAGO_ID) REFERENCES ROLES_PAGO (ID),
                                           CONSTRAINT CK_NOTIFICACIONES_ESTADO CHECK (ESTADO IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'OMITIDA')),
                                           CONSTRAINT CK_NOTIFICACIONES_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Cola de avisos por correo: un aviso por persona y periodo';

CREATE INDEX IDX_NOTIFICACIONES_DESPACHO ON NOTIFICACIONES_ROLES_PAGO (ESTADO, PROXIMO_INTENTO);

-- Dos acciones nuevas en la bitacora de acciones de RRHH
ALTER TABLE ACCIONES_ROLES_PAGO DROP CHECK CK_ACCIONES_ROLES_PAGO_TIPO;
ALTER TABLE ACCIONES_ROLES_PAGO ADD CONSTRAINT CK_ACCIONES_ROLES_PAGO_TIPO CHECK (TIPO_ACCION IN
                                                                                  ('CARGA_LOTE', 'ROL_REEMPLAZADO', 'ROL_INACTIVADO', 'ROL_ACTIVADO', 'PERIODO_PUBLICADO', 'REPORTE_EXPORTADO',
                                                                                   'NOTIFICACIONES_GENERADAS', 'NOTIFICACIONES_REINTENTADAS'));