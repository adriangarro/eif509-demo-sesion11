package cr.una.eif509.demo.web;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// La pantalla completa del guion, sin base de datos: controlador MVC +
// Thymeleaf + validación + Spring Security, con el servicio como doble.
// Cada prueba corresponde a un paso de la demo.
@WebMvcTest(ProductoWeb.class)
@Import(SeguridadConfig.class)
class ProductoWebTest {

    @Autowired MockMvc mvc;

    @MockBean ProductoService service;

    static final ProductoResumen CAFE = new ProductoResumen(5L, "Café", new BigDecimal("1500.00"), 20);

    // Paso 1: la lista. El controlador devuelve el nombre de una vista.
    @Test
    @WithMockUser(roles = "ADMIN")
    void laListaRenderizaLaPlantillaConLosDatosDelModelo() throws Exception {
        when(service.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(CAFE), PageRequest.of(0, 10), 1));

        mvc.perform(get("/admin/productos"))
                .andExpect(status().isOk())
                .andExpect(view().name("productos/lista"))
                .andExpect(model().attributeExists("pagina"))
                .andExpect(content().string(containsString("<td>Café</td>")))
                .andExpect(content().string(containsString("1500.00")))
                .andExpect(content().string(not(containsString("Té verde"))));   // fila de prototipo eliminada
    }

    // Paso 3: th:text escapa. El <script> llega como texto, no como código.
    @Test
    @WithMockUser(roles = "ADMIN")
    void unNombreConScriptSeMuestraEscapado() throws Exception {
        var malicioso = new ProductoResumen(99L, "<script>alert('x')</script>", BigDecimal.ONE, 1);
        when(service.listar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(malicioso), PageRequest.of(0, 10), 1));

        mvc.perform(get("/admin/productos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert"))));
    }

    // Paso 4: formulario vacío -> la misma vista con un error junto a cada campo.
    @Test
    @WithMockUser(roles = "ADMIN")
    void formularioVacioVuelveAMostrarseConLosErrores() throws Exception {
        mvc.perform(post("/admin/productos").with(csrf())
                        .param("nombre", "").param("precio", "").param("disponible", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("productos/form"))
                .andExpect(model().attributeHasFieldErrors("form", "nombre", "precio", "disponible"))
                .andExpect(content().string(containsString("Este campo es obligatorio.")))
                .andExpect(content().string(containsString("Hay errores en los campos marcados.")));

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void textoEnUnCampoNumericoDaUnMensajeEnEspanol() throws Exception {
        mvc.perform(post("/admin/productos").with(csrf())
                        .param("nombre", "Café").param("precio", "abc").param("disponible", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("form", "precio", "typeMismatch"))
                .andExpect(content().string(containsString("El valor debe ser un número.")));
    }

    // Paso 5: POST válido -> redirección (PRG) con mensaje flash.
    @Test
    @WithMockUser(roles = "ADMIN")
    void postValidoRedirigeConMensajeFlash() throws Exception {
        when(service.crear(any())).thenReturn(CAFE);

        mvc.perform(post("/admin/productos").with(csrf())
                        .param("nombre", "Café").param("precio", "1500").param("disponible", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/productos"))
                .andExpect(flash().attribute("ok", "El producto se creó correctamente."));

        verify(service).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nombreDuplicadoSeInformaJuntoAlCampo() throws Exception {
        when(service.crear(any())).thenThrow(new ProductoDuplicadoException("Café"));

        mvc.perform(post("/admin/productos").with(csrf())
                        .param("nombre", "Café").param("precio", "1500").param("disponible", "20"))
                .andExpect(status().isOk())
                .andExpect(view().name("productos/form"))
                .andExpect(model().attributeHasFieldErrorCode("form", "nombre", "duplicado"))
                .andExpect(content().string(containsString("Ya existe un producto con el nombre «Café»")));
    }

    // Paso 6: th:action insertó el token CSRF en el formulario...
    @Test
    @WithMockUser(roles = "ADMIN")
    void elFormularioIncluyeElTokenCsrf() throws Exception {
        mvc.perform(get("/admin/productos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    // ...y sin él, Spring Security rechaza el POST con 403.
    @Test
    @WithMockUser(roles = "ADMIN")
    void postSinTokenCsrfEsRechazadoCon403() throws Exception {
        mvc.perform(post("/admin/productos")
                        .param("nombre", "Café").param("precio", "1500").param("disponible", "20"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/admin/productos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editarCargaElFormularioConLosDatos() throws Exception {
        when(service.obtener(5L)).thenReturn(CAFE);

        mvc.perform(get("/admin/productos/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("productos/form"))
                // th:field escapa también los atributos: la «é» se escribe como &eacute;
                .andExpect(content().string(containsString("value=\"Caf&eacute;\"")))
                .andExpect(content().string(containsString("value=\"1500.00\"")))
                .andExpect(content().string(containsString("action=\"/admin/productos/5\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarRedirigeConMensajeFlash() throws Exception {
        when(service.actualizar(any(), any())).thenReturn(CAFE);

        mvc.perform(post("/admin/productos/5").with(csrf())
                        .param("nombre", "Café").param("precio", "1600").param("disponible", "18"))
                .andExpect(redirectedUrl("/admin/productos"))
                .andExpect(flash().attribute("ok", "El producto se actualizó correctamente."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void productoInexistenteMuestraLaPagina404() throws Exception {
        when(service.obtener(999L)).thenThrow(new ProductoNoExisteException(999L));

        mvc.perform(get("/admin/productos/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("No existe el producto 999")));
    }
}
