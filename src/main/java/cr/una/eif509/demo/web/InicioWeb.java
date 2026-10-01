package cr.una.eif509.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioWeb {

    // La raíz lleva al módulo administrativo.
    @GetMapping("/")
    public String inicio() {
        return "redirect:/admin/productos";
    }

    // Página propia de inicio de sesión, en español. El POST /login lo atiende
    // Spring Security; la plantilla usa th:action para incluir el token CSRF.
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
