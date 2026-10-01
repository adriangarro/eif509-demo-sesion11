-- Sesión 11: usuarios de la API con su rol. La clave se guarda con BCrypt
-- (función de derivación lenta y con sal); nunca en texto plano.
-- Claves de demostración: admin@demo.cr -> admin123, cliente@demo.cr -> cliente123.

CREATE TABLE usuario (
  id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  correo  TEXT NOT NULL UNIQUE,
  clave   TEXT NOT NULL,
  rol     TEXT NOT NULL CHECK (rol IN ('ADMIN', 'CLIENTE'))
);

INSERT INTO usuario (correo, clave, rol) VALUES
  ('admin@demo.cr',   '$2a$10$Pp/IczxTr2IZJ5bGWLzxZOLDpr7L1eul61ZkCEggBfaOPZN/.X7VW', 'ADMIN'),
  ('cliente@demo.cr', '$2a$10$.2VlzMqcv1JYJ3IWLloTJuvv1HRpe9SHM7ByTGIYcByBWz1ysl/L.', 'CLIENTE');

-- El usuario cliente@demo.cr es la clienta Ana Rojas: sus pedidos son los
-- que puede consultar (verificación de propiedad del recurso).
UPDATE cliente SET email = 'cliente@demo.cr' WHERE id = 1;
