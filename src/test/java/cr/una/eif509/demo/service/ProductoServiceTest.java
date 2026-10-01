package cr.una.eif509.demo.service;

import cr.una.eif509.demo.dto.ProductoForm;
import cr.una.eif509.demo.excepcion.ProductoDuplicadoException;
import cr.una.eif509.demo.excepcion.ProductoNoExisteException;
import cr.una.eif509.demo.model.Inventario;
import cr.una.eif509.demo.repository.InventarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Las reglas del catálogo, sin base de datos (Sesión 8): una prueba del
// camino feliz y una del camino en que la regla rechaza.
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock InventarioRepository inventarios;

    @InjectMocks ProductoService service;

    @Test
    void listaElCatalogoComoDtos() {
        var pageable = PageRequest.of(0, 10);
        when(inventarios.findAll(pageable)).thenReturn(new PageImpl<>(
                List.of(new Inventario("Café Tarrazú 500 g", 30, new BigDecimal("5200.00"))), pageable, 1));

        var pagina = service.listar(pageable);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).nombre()).isEqualTo("Café Tarrazú 500 g");
    }

    @Test
    void creaUnProductoConElNombreSinEspaciosSobrantes() {
        when(inventarios.existsByProductoIgnoreCase("Té verde")).thenReturn(false);
        when(inventarios.save(any(Inventario.class))).thenAnswer(inv -> inv.getArgument(0));

        var creado = service.crear(new ProductoForm("  Té verde  ", new BigDecimal("1200.00"), 35));

        assertThat(creado.nombre()).isEqualTo("Té verde");
        assertThat(creado.precio()).isEqualByComparingTo("1200.00");
        assertThat(creado.disponible()).isEqualTo(35);
    }

    @Test
    void rechazaUnNombreQueYaExiste() {
        when(inventarios.existsByProductoIgnoreCase("Teclado mecánico")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(
                new ProductoForm("Teclado mecánico", new BigDecimal("1.00"), 1)))
                .isInstanceOf(ProductoDuplicadoException.class);

        verify(inventarios, never()).save(any());
    }

    @Test
    void actualizaLosDatosDelProducto() {
        var existente = new Inventario("Café", 10, new BigDecimal("1500.00"));
        when(inventarios.findById(5L)).thenReturn(Optional.of(existente));
        when(inventarios.existsByProductoIgnoreCaseAndIdNot("Café molido", 5L)).thenReturn(false);

        var actualizado = service.actualizar(5L, new ProductoForm("Café molido", new BigDecimal("1800.00"), 12));

        assertThat(actualizado.nombre()).isEqualTo("Café molido");
        assertThat(existente.getPrecio()).isEqualByComparingTo("1800.00");
        assertThat(existente.getDisponible()).isEqualTo(12);
    }

    @Test
    void noActualizaConElNombreDeOtroProducto() {
        when(inventarios.findById(5L)).thenReturn(Optional.of(new Inventario("Café", 10, BigDecimal.TEN)));
        when(inventarios.existsByProductoIgnoreCaseAndIdNot("Monitor 27\"", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(5L,
                new ProductoForm("Monitor 27\"", BigDecimal.TEN, 1)))
                .isInstanceOf(ProductoDuplicadoException.class);
    }

    @Test
    void productoInexistenteEsUnErrorDeNegocioNombrado() {
        when(inventarios.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(999L)).isInstanceOf(ProductoNoExisteException.class);
        assertThatThrownBy(() -> service.actualizar(999L, new ProductoForm("X", BigDecimal.ONE, 1)))
                .isInstanceOf(ProductoNoExisteException.class);
    }
}
