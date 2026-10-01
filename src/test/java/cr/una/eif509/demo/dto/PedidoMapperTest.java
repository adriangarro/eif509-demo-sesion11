package cr.una.eif509.demo.dto;

import cr.una.eif509.demo.model.Cliente;
import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.model.Inventario;
import cr.una.eif509.demo.model.Pedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Prueba unitaria sin dobles: PedidoMapper es una función pura.
class PedidoMapperTest {

    @Test
    void copiaSoloLoQueLaVistaNecesita() {
        var pedido = new Pedido(new Cliente("Ana Rojas", "ana@mail.com"),
                new Inventario("Teclado mecánico", 10, new BigDecimal("15000.00")), 1, new BigDecimal("15000.00"));

        PedidoResumen resumen = PedidoMapper.aResumen(pedido);

        assertThat(resumen.cliente()).isEqualTo("Ana Rojas");
        assertThat(resumen.producto()).isEqualTo("Teclado mecánico");
        assertThat(resumen.total()).isEqualByComparingTo("15000.00");
        assertThat(resumen.estado()).isEqualTo(EstadoPedido.CREADO);
    }
}
