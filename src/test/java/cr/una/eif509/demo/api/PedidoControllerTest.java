package cr.una.eif509.demo.api;

import cr.una.eif509.demo.config.SeguridadConfig;
import cr.una.eif509.demo.dto.PedidoResumen;
import cr.una.eif509.demo.excepcion.InventarioInsuficienteException;
import cr.una.eif509.demo.excepcion.PedidoNoExisteException;
import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba del contrato HTTP, sin base de datos y sin Docker: @WebMvcTest
// levanta solo la capa web (controlador + manejador de errores +
// validación) y el servicio es un doble. Verifica códigos, cabeceras y
// el formato Problem Details: lo que un cliente de la API puede esperar.
// @Import(SeguridadConfig.class): con Spring Security en el classpath, la
// prueba debe usar las mismas cadenas de seguridad que la aplicación
// (la de /api/** es sin estado y sin CSRF).
@WebMvcTest(PedidoController.class)
@Import(SeguridadConfig.class)
class PedidoControllerTest {

    @Autowired MockMvc mvc;

    @MockBean PedidoService service;

    // Sesión 11: la API exige un token. jwt() simula una petición
    // autenticada con las autoridades indicadas, sin generar tokens reales.
    static final org.springframework.test.web.servlet.request.RequestPostProcessor ADMIN =
            jwt().jwt(j -> j.subject("admin@demo.cr")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

    static final PedidoResumen RESUMEN = new PedidoResumen(
            11L, "Ana Rojas", "Teclado mecánico", new BigDecimal("30000.00"), EstadoPedido.CREADO);

    @Test
    void postValidoDevuelve201ConLocationYElDto() throws Exception {
        when(service.crear(any())).thenReturn(RESUMEN);

        mvc.perform(post("/api/v1/pedidos").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": 1, "productoId": 1, "cantidad": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pedidos/11"))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.total").value(30000.00))
                .andExpect(jsonPath("$.estado").value("CREADO"));
    }

    // Formato en la frontera: @Valid falla antes de llamar al servicio.
    @Test
    void formatoInvalidoDevuelve400SinTocarElServicio() throws Exception {
        mvc.perform(post("/api/v1/pedidos").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId": 1, "cantidad": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Formato inválido"))
                .andExpect(jsonPath("$.errores.clienteId").value("Este campo es obligatorio."))
                .andExpect(jsonPath("$.errores.cantidad").value("El valor debe ser mayor que cero."));

        verifyNoInteractions(service);
    }

    // La excepción de negocio de la Sesión 7, ahora con código HTTP y
    // cuerpo estándar (RFC 9457).
    @Test
    void reglaDeNegocioDevuelve422EnFormatoProblemDetails() throws Exception {
        when(service.crear(any())).thenThrow(
                new InventarioInsuficienteException("Silla ergonómica", 2, 5));

        mvc.perform(post("/api/v1/pedidos").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": 1, "productoId": 3, "cantidad": 5}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.ejemplo.cr/errores/inventario-insuficiente"))
                .andExpect(jsonPath("$.title").value("Inventario insuficiente"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(
                        "Inventario insuficiente para «Silla ergonómica»: cantidad solicitada 5, cantidad disponible 2"))
                .andExpect(jsonPath("$.instance").value("/api/v1/pedidos"));
    }

    @Test
    void pedidoInexistenteDevuelve404() throws Exception {
        when(service.obtener(eq(999L), any())).thenThrow(new PedidoNoExisteException(999L));

        mvc.perform(get("/api/v1/pedidos/999").with(ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Pedido no encontrado"));
    }

    @Test
    void confirmarDosVecesDevuelve409() throws Exception {
        when(service.confirmar(11L)).thenThrow(new PedidoYaConfirmadoException(11L));

        mvc.perform(post("/api/v1/pedidos/11/confirmacion").with(ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Pedido ya confirmado"));
    }

    @Test
    void laColeccionRespondeUnaPaginaConMetadatos() throws Exception {
        when(service.listar(isNull(), any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(List.of(RESUMEN), PageRequest.of(0, 5), 1));

        mvc.perform(get("/api/v1/pedidos").with(ADMIN).param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cliente").value("Ana Rojas"))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
}
