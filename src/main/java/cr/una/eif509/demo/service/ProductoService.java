package cr.una.eif509.demo.service;

import cr.una.eif509.demo.dto.ProductoForm;
import cr.una.eif509.demo.dto.ProductoMapper;
import cr.una.eif509.demo.dto.ProductoResumen;
import cr.una.eif509.demo.excepcion.ProductoDuplicadoException;
import cr.una.eif509.demo.excepcion.ProductoNoExisteException;
import cr.una.eif509.demo.model.Inventario;
import cr.una.eif509.demo.repository.InventarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Un servicio y dos presentaciones: ProductoApi (JSON, Sesión 9) y
// ProductoWeb (HTML, Sesión 10) llaman a estos mismos métodos. La capa de
// negocio no depende de la forma en que se presentan los datos.
@Service
public class ProductoService {

    private final InventarioRepository inventarios;

    public ProductoService(InventarioRepository inventarios) {
        this.inventarios = inventarios;
    }

    @Transactional(readOnly = true)
    public Page<ProductoResumen> listar(Pageable pageable) {
        return inventarios.findAll(pageable).map(ProductoMapper::aResumen);
    }

    @Transactional(readOnly = true)
    public ProductoResumen obtener(Long id) {
        return inventarios.findById(id)
                .map(ProductoMapper::aResumen)
                .orElseThrow(() -> new ProductoNoExisteException(id));
    }

    // Regla de negocio: el nombre es único en el catálogo. El formato
    // (obligatorio, precio positivo...) ya lo validó @Valid en la frontera.
    @Transactional
    public ProductoResumen crear(ProductoForm form) {
        String nombre = form.getNombre().trim();
        if (inventarios.existsByProductoIgnoreCase(nombre)) {
            throw new ProductoDuplicadoException(nombre);
        }
        var producto = inventarios.save(
                new Inventario(nombre, form.getDisponible(), form.getPrecio()));
        return ProductoMapper.aResumen(producto);
    }

    @Transactional
    public ProductoResumen actualizar(Long id, ProductoForm form) {
        var producto = inventarios.findById(id)
                .orElseThrow(() -> new ProductoNoExisteException(id));
        String nombre = form.getNombre().trim();
        if (inventarios.existsByProductoIgnoreCaseAndIdNot(nombre, id)) {
            throw new ProductoDuplicadoException(nombre);
        }
        producto.actualizarDatos(nombre, form.getPrecio(), form.getDisponible());
        return ProductoMapper.aResumen(producto);
    }
}
