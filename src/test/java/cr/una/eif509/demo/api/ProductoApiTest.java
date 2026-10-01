package cr.una.eif509.demo.api;

import cr.una.eif509.demo.config.SeguridadConfig;
import cr.una.eif509.demo.dto.ProductoResumen;
import cr.una.eif509.demo.excepcion.ProductoDuplicadoException;
import cr.una.eif509.demo.excepcion.ProductoNoExisteException;
import cr.una.eif509.demo.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// La API del catálogo: el mismo DTO de entrada que el formulario HTML,
// pero los errores se informan como Problem Details (Sesión 9).
@WebMvcTest(ProductoApi.class)
@Import(SeguridadConfig.class)
class ProductoApiTest {

    @Autowired MockMvc mvc;

    @MockBean ProductoService service;

    static final ProductoResumen CAFE = new ProductoResumen(13L, "Café Tarrazú 1 kg", new BigDecimal("9800.00"), 25);

    @Test
    void laColeccionEsJsonPaginado() throws Exception {
        when(service.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(CAFE), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].nombre").value("Café Tarrazú 1 kg"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void postValidoDevuelve201ConLocation() throws Exception {
        when(service.crear(any())).thenReturn(CAFE);

        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Café Tarrazú 1 kg", "precio": 9800.00, "disponible": 25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/productos/13"))
                .andExpect(jsonPath("$.id").value(13));
    }

    // Mismos @NotBlank y @Positive que el formulario; aquí, un 400.
    // Y sin token CSRF: la cadena de /api/** no lo exige.
    @Test
    void formatoInvalidoDevuelve400ConLosMismosMensajesQueElFormulario() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "", "precio": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.nombre").value("Este campo es obligatorio."))
                .andExpect(jsonPath("$.errores.precio").value("El valor debe ser mayor que cero."))
                .andExpect(jsonPath("$.errores.disponible").value("Este campo es obligatorio."));

        verifyNoInteractions(service);
    }

    @Test
    void nombreDuplicadoDevuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new ProductoDuplicadoException("Teclado mecánico"));

        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Teclado mecánico", "precio": 1, "disponible": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Producto duplicado"));
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        when(service.obtener(999L)).thenThrow(new ProductoNoExisteException(999L));

        mvc.perform(get("/api/v1/productos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Producto no encontrado"));
    }
}
