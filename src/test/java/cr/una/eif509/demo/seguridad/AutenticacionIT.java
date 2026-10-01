package cr.una.eif509.demo.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.una.eif509.demo.PostgresContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El recorrido de la Demostración 1 con tokens reales, contra PostgreSQL
// (Testcontainers): inicio de sesión, 401, 200, 403 por rol, 403 por
// propiedad del recurso y CORS.
@AutoConfigureMockMvc
class AutenticacionIT extends PostgresContainerBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtDecoder decoder;

    @Test
    void elInicioDeSesionEmiteUnTokenConElCorreoYElRol() throws Exception {
        String token = login("admin@demo.cr", "admin123");

        assertThat(token.split("\\.")).hasSize(3);   // header.payload.firma
        var jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("admin@demo.cr");
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of("ADMIN"));
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("eif509");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    // El mismo mensaje para una clave incorrecta y para un correo que no
    // existe: no se revela qué correos están registrados.
    @Test
    void credencialesIncorrectas_responden401ConElMismoMensaje() throws Exception {
        for (String cuerpo : List.of(
                "{\"correo\": \"admin@demo.cr\", \"clave\": \"incorrecta\"}",
                "{\"correo\": \"nadie@demo.cr\", \"clave\": \"admin123\"}")) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Credenciales inválidas"))
                    .andExpect(jsonPath("$.detail").value("Correo o clave incorrectos"));
        }
    }

    @Test
    void sinTokenEs401YConTokenEs200() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/productos").header("Authorization", bearer("admin@demo.cr", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void elRolClienteNoPuedeCrearProductos() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .header("Authorization", bearer("cliente@demo.cr", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Producto del cliente\", \"precio\": 1, \"disponible\": 1}"))
                .andExpect(status().isForbidden());
    }

    // cliente@demo.cr es Ana Rojas (V7): el pedido 1 es suyo y el 3 es de Luis Mora.
    @Test
    void unClienteSoloVeSusPropiosPedidos() throws Exception {
        String cliente = bearer("cliente@demo.cr", "cliente123");

        mvc.perform(get("/api/v1/pedidos/1").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Ana Rojas"));

        mvc.perform(get("/api/v1/pedidos/3").header("Authorization", cliente))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/pedidos").param("size", "100").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].cliente", everyItem(is("Ana Rojas"))));

        // El administrador ve cualquier pedido.
        mvc.perform(get("/api/v1/pedidos/3").header("Authorization", bearer("admin@demo.cr", "admin123")))
                .andExpect(status().isOk());
    }

    // CORS: la petición previa (OPTIONS) del navegador desde la SPA se
    // autoriza; la de un origen no configurado se rechaza.
    @Test
    void corsAutorizaSoloElOrigenDeLaSpa() throws Exception {
        mvc.perform(options("/api/v1/productos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/api/v1/productos")
                        .header("Origin", "http://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private String login(String correo, String clave) throws Exception {
        var respuesta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"" + correo + "\", \"clave\": \"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("token").asText();
    }

    private String bearer(String correo, String clave) throws Exception {
        return "Bearer " + login(correo, clave);
    }
}
