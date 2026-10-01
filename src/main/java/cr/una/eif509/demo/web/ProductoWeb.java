package cr.una.eif509.demo.web;

import cr.una.eif509.demo.dto.ProductoForm;
import cr.una.eif509.demo.excepcion.ProductoDuplicadoException;
import cr.una.eif509.demo.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// La presentación para personas: @Controller (no @RestController).
// Cada método devuelve el nombre lógico de una vista; el ViewResolver lo
// resuelve a templates/<nombre>.html y Thymeleaf la renderiza con el Model.
// Igual que el controlador REST, no contiene lógica de negocio: recibe la
// petición, llama al mismo servicio, agrega los DTOs al modelo y elige la
// vista.
@Controller
@RequestMapping("/admin/productos")
public class ProductoWeb {

    private static final String LISTA = "productos/lista";
    private static final String FORMULARIO = "productos/form";

    private final ProductoService service;

    public ProductoWeb(ProductoService service) {
        this.service = service;
    }

    // Paso 1 de la demo: la lista. service.listar(pageable) es la misma
    // línea que usa ProductoApi; solo cambia lo que se hace con el resultado.
    // Orden por defecto: del más reciente al más antiguo, para que el último
    // producto creado aparezca en la primera fila.
    @GetMapping
    public String listar(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        model.addAttribute("pagina", service.listar(pageable));
        return LISTA;   // -> templates/productos/lista.html
    }

    // Paso 4: el formulario vacío, enlazado a un DTO vacío (th:object).
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ProductoForm());
        return FORMULARIO;
    }

    // Pasos 4 y 5: validación y Post/Redirect/Get.
    // BindingResult va inmediatamente después del objeto @Valid: si no,
    // Spring lanza la excepción de validación (400) en lugar de entregar
    // los errores a este método.
    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ProductoForm form,
                        BindingResult errores,
                        RedirectAttributes flash) {
        if (errores.hasErrors()) {
            return FORMULARIO;   // la misma vista, ahora con los mensajes por campo
        }
        try {
            service.crear(form);
        } catch (ProductoDuplicadoException e) {
            // En MVC, el error se informa en el propio formulario: el
            // controlador convierte la excepción de negocio en un error del
            // campo. En la API, esa conversión la hace el manejador global
            // (409 Problem Details).
            errores.rejectValue("nombre", "duplicado", e.getMessage());
            return FORMULARIO;
        }
        // Los atributos del modelo se pierden en una redirección; un atributo
        // flash se conserva durante exactamente una redirección.
        flash.addFlashAttribute("ok", "El producto se creó correctamente.");
        return "redirect:/admin/productos";   // PRG: la recarga repite el GET, no el POST
    }

    @GetMapping("/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", ProductoForm.desde(service.obtener(id)));
        model.addAttribute("productoId", id);
        return FORMULARIO;
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") ProductoForm form,
                             BindingResult errores,
                             Model model,
                             RedirectAttributes flash) {
        model.addAttribute("productoId", id);
        if (errores.hasErrors()) {
            return FORMULARIO;
        }
        try {
            service.actualizar(id, form);
        } catch (ProductoDuplicadoException e) {
            errores.rejectValue("nombre", "duplicado", e.getMessage());
            return FORMULARIO;
        }
        flash.addFlashAttribute("ok", "El producto se actualizó correctamente.");
        return "redirect:/admin/productos";
    }
}
