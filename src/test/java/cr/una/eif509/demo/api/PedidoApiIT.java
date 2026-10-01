package cr.una.eif509.demo.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.una.eif509.demo.PostgresContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
        var creado = http.postForEntity("/api/v1/pedidos",
                cuerpo("{\"clienteId\": 1, \"productoId\": 1, \"cantidad\": 2}"), String.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = creado.getHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(location).startsWith("/api/v1/pedidos/");
        assertThat(leer(creado).get("total").decimalValue()).isEqualByComparingTo("30000.00");

        // GET del recurso recién creado -> 200
        var obtenido = http.getForEntity(location, String.class);
        assertThat(obtenido.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leer(obtenido).get("estado").asText()).isEqualTo("CREADO");

        // POST /confirmacion -> 200 y estado CONFIRMADO (Sesión 7 por HTTP)
        var confirmado = http.postForEntity(location + "/confirmacion", null, String.class);
        assertThat(confirmado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leer(confirmado).get("estado").asText()).isEqualTo("CONFIRMADO");

        // Otra vez -> 409 Conflict
        var otraVez = http.postForEntity(location + "/confirmacion", null, String.class);
        assertThat(otraVez.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(leer(otraVez).get("title").asText()).isEqualTo("Pedido ya confirmado");
    }

    @Test
    void sinExistenciasDevuelve422EnFormatoProblemDetails() throws Exception {
        var respuesta = http.postForEntity("/api/v1/pedidos",
                cuerpo("{\"clienteId\": 1, \"productoId\": 3, \"cantidad\": 5}"), String.class);

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
        var creado = http.postForEntity("/api/v1/pedidos",
                cuerpo("{\"clienteId\": 2, \"productoId\": 4, \"cantidad\": 1}"), String.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var confirmado = http.postForEntity(
                creado.getHeaders().getFirst(HttpHeaders.LOCATION) + "/confirmacion", null, String.class);

        assertThat(confirmado.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(leer(confirmado).get("title").asText()).isEqualTo("Monto de facturación excedido");
    }

    @Test
    void pedidoInexistenteDevuelve404() {
        var respuesta = http.getForEntity("/api/v1/pedidos/999", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void laColeccionVienePaginadaYOrdenada() throws Exception {
        var respuesta = http.getForEntity(
                "/api/v1/pedidos?page=0&size=5&sort=creadoEn,desc", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        var cuerpo = leer(respuesta);
        assertThat(cuerpo.get("content")).hasSize(5);
        assertThat(cuerpo.get("page").get("size").asInt()).isEqualTo(5);
        assertThat(cuerpo.get("page").get("totalElements").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(cuerpo.get("page").get("totalPages").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void ordenarPorUnCampoInexistenteDevuelve400() throws Exception {
        var respuesta = http.getForEntity("/api/v1/pedidos?sort=fecha,desc", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(leer(respuesta).get("title").asText()).isEqualTo("Parámetro de ordenamiento inválido");
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
