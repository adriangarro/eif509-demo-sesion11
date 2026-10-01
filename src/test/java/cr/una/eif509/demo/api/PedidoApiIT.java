package cr.una.eif509.demo.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.una.eif509.demo.PostgresContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// De punta a punta: cliente HTTP real -> controlador -> servicio
// (@Transactional) -> repositorio -> PostgreSQL real (Testcontainers).
// Son pocas pruebas y más lentas, en la parte superior de la pirámide de
// pruebas. Las reglas ya se verificaron con pruebas unitarias; estas
// comprueban que las tres capas funcionan conectadas.
class PedidoApiIT extends PostgresContainerBase {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;

    @Test
    void crearConsultarYConfirmarUnPedidoDePuntaAPunta() throws Exception {
        // POST -> 201 + Location
        var creado = post("/api/v1/pedidos", "{\"clienteId\": 1, \"productoId\": 1, \"cantidad\": 2}");
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = creado.getHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(location).startsWith("/api/v1/pedidos/");
        assertThat(leer(creado).get("total").decimalValue()).isEqualByComparingTo("30000.00");

        // GET del recurso recién creado -> 200
        var obtenido = get(location);
        assertThat(obtenido.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leer(obtenido).get("estado").asText()).isEqualTo("CREADO");

        // POST /confirmacion -> 200 y estado CONFIRMADO (Sesión 7 por HTTP)
        var confirmado = post(location + "/confirmacion", null);
        assertThat(confirmado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leer(confirmado).get("estado").asText()).isEqualTo("CONFIRMADO");

        // Otra vez -> 409 Conflict
        var otraVez = post(location + "/confirmacion", null);
        assertThat(otraVez.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(leer(otraVez).get("title").asText()).isEqualTo("Pedido ya confirmado");
    }

    @Test
    void sinExistenciasDevuelve422EnFormatoProblemDetails() throws Exception {
        var respuesta = post("/api/v1/pedidos", "{\"clienteId\": 1, \"productoId\": 3, \"cantidad\": 5}");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        var cuerpo = leer(respuesta);
        assertThat(cuerpo.get("title").asText()).isEqualTo("Inventario insuficiente");
        assertThat(cuerpo.get("detail").asText()).contains("cantidad solicitada 5, cantidad disponible 2");
    }

    @Test
    void montoSobreElLimiteAlConfirmarDevuelve422() throws Exception {
        // Una laptop (66 000) supera el límite de facturación automática.
        var creado = post("/api/v1/pedidos", "{\"clienteId\": 2, \"productoId\": 4, \"cantidad\": 1}");
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var confirmado = post(creado.getHeaders().getFirst(HttpHeaders.LOCATION) + "/confirmacion", null);

        assertThat(confirmado.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(leer(confirmado).get("title").asText()).isEqualTo("Monto de facturación excedido");
    }

    @Test
    void pedidoInexistenteDevuelve404() throws Exception {
        var respuesta = get("/api/v1/pedidos/999");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void laColeccionVienePaginadaYOrdenada() throws Exception {
        var respuesta = get("/api/v1/pedidos?page=0&size=5&sort=creadoEn,desc");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        var cuerpo = leer(respuesta);
        assertThat(cuerpo.get("content")).hasSize(5);
        assertThat(cuerpo.get("page").get("size").asInt()).isEqualTo(5);
        assertThat(cuerpo.get("page").get("totalElements").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(cuerpo.get("page").get("totalPages").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void ordenarPorUnCampoInexistenteDevuelve400() throws Exception {
        var respuesta = get("/api/v1/pedidos?sort=fecha,desc");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(leer(respuesta).get("title").asText()).isEqualTo("Parámetro de ordenamiento inválido");
    }

    private String tokenAdmin;

    // Inicio de sesión real contra la base (usuario de la migración V7).
    private String tokenAdmin() throws Exception {
        if (tokenAdmin == null) {
            var r = http.postForEntity("/auth/login",
                    cuerpo("{\"correo\": \"admin@demo.cr\", \"clave\": \"admin123\"}"), String.class);
            tokenAdmin = leer(r).get("token").asText();
        }
        return tokenAdmin;
    }

    private ResponseEntity<String> post(String url, String jsonBody) throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenAdmin());
        return http.exchange(url, HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class);
    }

    private ResponseEntity<String> get(String url) throws Exception {
        var headers = new HttpHeaders();
        headers.setBearerAuth(tokenAdmin());
        return http.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private static HttpEntity<String> cuerpo(String jsonBody) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonBody, headers);
    }

    private JsonNode leer(ResponseEntity<String> respuesta) throws Exception {
        return json.readTree(respuesta.getBody());
    }
}
