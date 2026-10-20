CREATE TABLE ASIGNACIONES_ROL (
                                  ID                 BIGINT       NOT NULL AUTO_INCREMENT,
                                  USERNAME_AD        VARCHAR(50)  NOT NULL,
                                  ROL                VARCHAR(10)  NOT NULL,
                                  VER_ROLES_AJENOS   INT          NOT NULL DEFAULT 1,
                                  ESTADO_REGISTRO    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
                                  CREADO_POR         VARCHAR(100) NOT NULL,
                                  CREADO_EN          DATETIME(6)  NOT NULL,
                                  MODIFICADO_POR     VARCHAR(100) NOT NULL,
                                  MODIFICADO_EN      DATETIME(6)  NOT NULL,
                                  VERSION            BIGINT       NOT NULL DEFAULT 0,
                                  CONSTRAINT PK_ASIGNACIONES_ROL PRIMARY KEY (ID),
                                  CONSTRAINT UK_ASIGNACIONES_ROL_USUARIO_ROL UNIQUE (USERNAME_AD, ROL),
                                  CONSTRAINT CK_ASIGNACIONES_ROL_ROL CHECK (ROL IN ('ADMIN', 'RRHH')),
                                  CONSTRAINT CK_ASIGNACIONES_ROL_VER_AJENOS CHECK (VER_ROLES_AJENOS IN (0, 1)),
                                  CONSTRAINT CK_ASIGNACIONES_ROL_ESTADO_REGISTRO CHECK (ESTADO_REGISTRO IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Roles elevados (ADMIN, RRHH) de un codigo de usuario; EMPLEADO lo es todo usuario activo';

-- Dos acciones nuevas en la bitacora de acciones
ALTER TABLE ACCIONES_ROLES_PAGO DROP CHECK CK_ACCIONES_ROLES_PAGO_TIPO;
ALTER TABLE ACCIONES_ROLES_PAGO ADD CONSTRAINT CK_ACCIONES_ROLES_PAGO_TIPO CHECK (TIPO_ACCION IN
                                                                                  ('CARGA_LOTE', 'ROL_REEMPLAZADO', 'ROL_INACTIVADO', 'ROL_ACTIVADO', 'PERIODO_PUBLICADO', 'REPORTE_EXPORTADO',
                                                                                   'NOTIFICACIONES_GENERADAS', 'NOTIFICACIONES_REINTENTADAS', 'USUARIO_ROL_OTORGADO', 'USUARIO_ROL_REVOCADO'));