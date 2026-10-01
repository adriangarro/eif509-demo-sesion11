-- Sesión 9: el precio vive en el inventario. El total del pedido se
-- CALCULA en el servicio (precio × cantidad); nunca lo envía el cliente.
-- Se agrega con DEFAULT para que las filas existentes sigan siendo válidas.

ALTER TABLE inventario
  ADD COLUMN precio NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio >= 0);

UPDATE inventario SET precio = CASE producto
    WHEN 'Teclado mecánico' THEN 15000.00
    WHEN 'Monitor 27"'      THEN 42500.00
    WHEN 'Silla ergonómica' THEN 61000.00
    WHEN 'Laptop 14"'       THEN 66000.00
  END;
