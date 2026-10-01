package cr.una.eif509.demo.web;

import cr.una.eif509.demo.PostgresContainerBase;
import cr.una.eif509.demo.model.Inventario;
import cr.una.eif509.demo.repository.InventarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// De punta a punta contra PostgreSQL real (Testcontainers): el formulario
// escribe en la base de datos y el paso 7 de la demo se verifica con una
// prueba automática: un servicio y dos presentaciones (HTML y JSON) que
// muestran el mismo dato.
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class AdminProductosIT extends PostgresContainerBase {

    @Autowired MockMvc mvc;
    @Autowired InventarioRepository inventarios;

    @Test
    void loCreadoEnElFormularioApareceEnLaListaYEnLaApi() throws Exception {
        mvc.perform(post("/admin/productos").with(csrf())
                        .param("nombre", "Producto de la prueba MVC")
                        .param("precio", "4321.00")
                        .param("disponible", "7"))
                .andExpect(redirectedUrl("/admin/productos"));

        assertThat(inventarios.existsByProductoIgnoreCase("Producto de la prueba MVC")).isTrue();

        mvc.perform(get("/admin/productos").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Producto de la prueba MVC")));

        mvc.perform(get("/api/v1/productos").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.nombre == 'Producto de la prueba MVC')].precio").value(4321.00));
    }

    @Test
    void unScriptGuardadoEnLaBaseSeMuestraComoTexto() throws Exception {
        inventarios.save(new Inventario("<script>alert('it')</script>", 1, BigDecimal.ONE));

        mvc.perform(get("/admin/productos").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;it&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert"))));
    }

    @Test
    void unNombreRepetidoNoLlegaALaBase() throws Exception {
        long antes = inventarios.count();

        mvc.perform(post("/admin/productos").with(csrf())
                        .param("nombre", "teclado MECÁNICO")
                        .param("precio", "1").param("disponible", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe un producto")));

        assertThat(inventarios.count()).isEqualTo(antes);
    }

    @Test
    void sinTokenCsrfNadaSeGuarda() throws Exception {
        long antes = inventarios.count();

        mvc.perform(post("/admin/productos")
                        .param("nombre", "Producto sin CSRF")
                        .param("precio", "1").param("disponible", "1"))
                .andExpect(status().isForbidden());

        assertThat(inventarios.count()).isEqualTo(antes);
    }
}
