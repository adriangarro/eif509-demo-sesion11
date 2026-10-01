package cr.una.eif509.demo.service;

import cr.una.eif509.demo.excepcion.MontoFacturaExcedidoException;
import cr.una.eif509.demo.model.Cliente;
import cr.una.eif509.demo.model.Factura;
import cr.una.eif509.demo.model.Inventario;
import cr.una.eif509.demo.model.Pedido;
import cr.una.eif509.demo.repository.FacturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cada regla de negocio: una prueba del camino feliz y una del camino
// en que la regla rechaza. Aquí, la regla del límite de facturación.
@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock FacturaRepository facturas;

    @InjectMocks FacturaService service;

    @Test
    void creaLaFacturaPorElTotalDelPedido() {
        when(facturas.saveAndFlush(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(pedidoPor("15000.00"));

        verify(facturas).saveAndFlush(any(Factura.class));
    }

    @Test
    void rechazaMontoSobreElLimiteDeFacturacionAutomatica() {
        assertThatThrownBy(() -> service.crear(pedidoPor("66000.00")))
                .isInstanceOf(MontoFacturaExcedidoException.class);

        verify(facturas, never()).saveAndFlush(any());
    }

    private static Pedido pedidoPor(String total) {
        return new Pedido(new Cliente("Ana Rojas", "ana@mail.com"),
                new Inventario("Laptop 14\"", 3, new BigDecimal("66000.00")), 1, new BigDecimal(total));
    }
}
