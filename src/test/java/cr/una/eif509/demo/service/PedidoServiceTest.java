package cr.una.eif509.demo.service;

import cr.una.eif509.demo.dto.CrearPedidoRequest;
import cr.una.eif509.demo.excepcion.ReferenciaInvalidaException;
import cr.una.eif509.demo.excepcion.InventarioInsuficienteException;
import cr.una.eif509.demo.excepcion.PedidoNoExisteException;
import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import cr.una.eif509.demo.model.Cliente;
import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.model.Inventario;
import cr.una.eif509.demo.model.Pedido;
import cr.una.eif509.demo.repository.ClienteRepository;
import cr.una.eif509.demo.repository.InventarioRepository;
import cr.una.eif509.demo.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Prueba unitaria del servicio: sin Spring, sin base de datos, sin Docker.
// Mockito crea dobles de los colaboradores y construye el servicio
// inyectándolos por constructor. Se prueba la regla en milisegundos.
//
// (La garantía transaccional —el rollback— no se prueba aquí: con
// repositorios simulados no hay transacción que deshacer. Eso lo
// verifica PedidoServiceIT con Testcontainers.)
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    // Un @Mock por cada colaborador del constructor de PedidoService.
    // Si falta uno, @InjectMocks deja ese parámetro en null.
    @Mock PedidoRepository pedidos;
    @Mock ClienteRepository clientes;
    @Mock InventarioRepository inventarios;
    @Mock FacturaService facturas;
    @Mock BitacoraService bitacora;
    @Mock AuditoriaService auditoria;

    @InjectMocks PedidoService service;

    // Camino feliz: hay pedido, hay inventario -> se confirma y se factura.
    @Test
    void confirmaCuandoHayInventario() {
        // Stubs: preparan el escenario (se piden 2 y hay 10).
        when(pedidos.findById(1L)).thenReturn(Optional.of(pedidoCon(2, 10)));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.confirmarPedido(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(resultado.getProducto().getDisponible()).isEqualTo(8);
        // Mock: se verifica la interacción porque esa interacción es la
        // regla: al confirmar, se factura.
        verify(facturas).crear(any(Pedido.class));
    }

    // Camino de la regla: se piden 5 y hay 2, por lo que se lanza la
    // excepción y no se factura nada. Esta aserción detecta el error de
    // facturar antes de validar.
    @Test
    void rechazaSinInventario() {
        when(pedidos.findById(1L)).thenReturn(Optional.of(pedidoCon(5, 2)));

        assertThatThrownBy(() -> service.confirmarPedido(1L))
                .isInstanceOf(InventarioInsuficienteException.class);

        verify(facturas, never()).crear(any());
    }

    @Test
    void rechazaPedidoInexistente() {
        when(pedidos.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarPedido(99L))
                .isInstanceOf(PedidoNoExisteException.class);

        verify(facturas, never()).crear(any());
    }

    @Test
    void rechazaPedidoYaConfirmado() {
        Pedido yaConfirmado = pedidoCon(1, 10);
        yaConfirmado.confirmar();
        when(pedidos.findById(1L)).thenReturn(Optional.of(yaConfirmado));

        assertThatThrownBy(() -> service.confirmarPedido(1L))
                .isInstanceOf(PedidoYaConfirmadoException.class);

        verify(facturas, never()).crear(any());
    }

    // Sesión 9 · Caso de uso «crear»: el total lo calcula el servicio.
    @Test
    void creaUnPedidoConElTotalCalculado() {
        when(clientes.findById(1L)).thenReturn(Optional.of(new Cliente("Ana Rojas", "ana@mail.com")));
        when(inventarios.findById(1L)).thenReturn(Optional.of(
                new Inventario("Teclado mecánico", 10, new BigDecimal("15000.00"))));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        var resumen = service.crear(new CrearPedidoRequest(1L, 1L, 2));

        assertThat(resumen.total()).isEqualByComparingTo("30000.00");
        assertThat(resumen.estado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(resumen.producto()).isEqualTo("Teclado mecánico");
    }

    // Regla de negocio al crear: no se acepta un pedido sin existencias.
    @Test
    void noCreaUnPedidoSinExistencias() {
        when(clientes.findById(1L)).thenReturn(Optional.of(new Cliente("Ana Rojas", "ana@mail.com")));
        when(inventarios.findById(3L)).thenReturn(Optional.of(
                new Inventario("Silla ergonómica", 2, new BigDecimal("61000.00"))));

        assertThatThrownBy(() -> service.crear(new CrearPedidoRequest(1L, 3L, 5)))
                .isInstanceOf(InventarioInsuficienteException.class);

        verify(pedidos, never()).save(any());
    }

    @Test
    void rechazaCrearParaUnClienteInexistente() {
        when(clientes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new CrearPedidoRequest(99L, 1L, 1)))
                .isInstanceOf(ReferenciaInvalidaException.class);

        verify(pedidos, never()).save(any());
    }

    // Método auxiliar: construye un pedido de prueba con su producto en
    // memoria. Permite describir el escenario de cada prueba en una sola línea.
    private static Pedido pedidoCon(int cantidad, int disponible) {
        var cliente = new Cliente("Ana Rojas", "ana@mail.com");
        var producto = new Inventario("Teclado mecánico", disponible, new BigDecimal("15000.00"));
        return new Pedido(cliente, producto, cantidad, new BigDecimal("15000.00"));
    }
}
