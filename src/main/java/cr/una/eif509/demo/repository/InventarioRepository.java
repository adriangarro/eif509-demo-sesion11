package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;

// El catálogo de productos se almacena en la tabla inventario (Sesión 7):
// cada fila es un producto con su nombre, precio y existencias.
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    // Métodos derivados del nombre para la regla «nombre único».
    boolean existsByProductoIgnoreCase(String producto);

    boolean existsByProductoIgnoreCaseAndIdNot(String producto, Long id);
}
