-- Sesión 10: más productos en el catálogo, para que la lista del módulo
-- administrativo tenga paginación (12 productos, 10 por página).
-- Los 4 productos de la V4 siguen igual: los pedidos los referencian.

INSERT INTO inventario (producto, disponible, precio) VALUES
  ('Mouse inalámbrico',        25,  9500.00),
  ('Audífonos con micrófono',  12, 18900.00),
  ('Cámara web HD',             8, 27500.00),
  ('Disco SSD 1 TB',            6, 48900.00),
  ('Memoria USB 64 GB',        40,  6500.00),
  ('Base para laptop',         15, 14200.00),
  ('Lámpara de escritorio',     9, 21000.00),
  ('Café Tarrazú 500 g',       30,  5200.00);
