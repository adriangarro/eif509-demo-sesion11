package cr.una.eif509.demo.web;

import cr.una.eif509.demo.excepcion.ProductoNoExisteException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

// Manejador de errores de las vistas. Cumple la misma función que
// ManejadorErrores en la API, pero responde con una página HTML y el código
// HTTP correcto, en lugar de un cuerpo application/problem+json. Se limita
// al paquete web.
@ControllerAdvice(basePackageClasses = ManejadorErroresWeb.class)
public class ManejadorErroresWeb {

    @ExceptionHandler(ProductoNoExisteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String productoNoExiste(ProductoNoExisteException e, Model model) {
        model.addAttribute("mensaje", e.getMessage());
        return "error/404";
    }
}
