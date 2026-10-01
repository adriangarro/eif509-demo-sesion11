package cr.una.eif509.demo.api;

import cr.una.eif509.demo.config.SeguridadConfig;
import cr.una.eif509.demo.dto.ProductoResumen;
import cr.una.eif509.demo.excepcion.AccesoDenegadoException;
import cr.una.eif509.demo.service.PedidoService;
import cr.una.eif509.demo.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Paso 4 de la lámina: pruebas de seguridad (401, 403 y 200), sin base de
// datos. jwt() de spring-security-test simula una petición autenticada con
// las autoridades indicadas, sin generar tokens reales.
@WebMvcTest({ProductoApi.class, PedidoController.class})
@Import(SeguridadConfig.class)
class SeguridadApiTest {

    @Autowired MockMvc mvc;

    @MockBean ProductoService productos;
    @MockBean PedidoService pedidos;

    static final String PRODUCTO_JSON = """
            {"nombre": "Café Tarrazú 1 kg", "precio": 9800.00, "disponible": 25}
            """;

    @Test
    void sinToken_responde401() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));

        verifyNoInteractions(productos);
    }

    @Test
    void inclusoLaLecturaExigeToken() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isUnauthorized());
    }

    // Un token mal formado (o con una firma que no corresponde) no se acepta.
    @Test
    void tokenInvalido_responde401() throws Exception {
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer esto.no.es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    // Autenticado, pero su rol no le permite crear productos.
    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acceso denegado"));

        verifyNoInteractions(productos);
    }

    @Test
    void adminCreaProducto() throws Exception {
        when(productos.crear(any())).thenReturn(
                new ProductoResumen(13L, "Café Tarrazú 1 kg", new BigDecimal("9800.00"), 25));

        mvc.perform(post("/api/v1/productos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
                .andExpect(status().isCreated());
    }

    // El rol CLIENTE sí puede leer el catálogo.
    @Test
    void clientePuedeListarProductos() throws Exception {
        when(productos.listar(any())).thenReturn(org.springframework.data.domain.Page.empty());

        mvc.perform(get("/api/v1/productos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    // La propiedad del recurso la decide el servicio; aquí se verifica que
    // su excepción llega al cliente como 403 Problem Details.
    @Test
    void pedidoDeOtroCliente_responde403() throws Exception {
        when(pedidos.obtener(eq(3L), any())).thenThrow(new AccesoDenegadoException());

        mvc.perform(get("/api/v1/pedidos/3")
                        .with(jwt().jwt(j -> j.subject("cliente@demo.cr"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tiene permiso para consultar este recurso"));
    }
}
