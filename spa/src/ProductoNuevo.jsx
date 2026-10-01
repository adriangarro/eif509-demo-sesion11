import { useState } from "react";
import { api } from "./api.js";

// Formulario para POST /api/v1/productos. La SPA valida lo básico para
// ayudar al usuario (campos requeridos), pero la validación que protege
// el sistema es la de la API. Con el rol CLIENTE, la API responde 403 y el
// mensaje del Problem Details se muestra aquí.
export default function ProductoNuevo({ alCrear }) {
  const [nombre, setNombre] = useState("");
  const [precio, setPrecio] = useState("");
  const [disponible, setDisponible] = useState("");
  const [mensaje, setMensaje] = useState(null);

  async function enviar(evento) {
    evento.preventDefault();
    setMensaje(null);
    try {
      const creado = await api("/api/v1/productos", {
        method: "POST",
        body: JSON.stringify({ nombre, precio: Number(precio), disponible: Number(disponible) }),
      });
      setMensaje({ tipo: "ok", texto: `Se creó el producto «${creado.nombre}».` });
      setNombre("");
      setPrecio("");
      setDisponible("");
      alCrear();
    } catch (problema) {
      // 400 trae el detalle por campo en "errores"; 403 y 409, en "detail".
      const porCampo = problema.errores ? Object.values(problema.errores).join(" ") : "";
      setMensaje({ tipo: "error", texto: `${problema.title}: ${problema.detail ?? ""} ${porCampo}`.trim() });
    }
  }

  return (
    <section>
      <h2>Nuevo producto</h2>
      {mensaje && <p className={mensaje.tipo}>{mensaje.texto}</p>}
      <form onSubmit={enviar} className="formulario en-linea">
        <input placeholder="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />
        <input placeholder="Precio" type="number" step="0.01" value={precio}
               onChange={(e) => setPrecio(e.target.value)} required />
        <input placeholder="Existencias" type="number" step="1" value={disponible}
               onChange={(e) => setDisponible(e.target.value)} required />
        <button type="submit">Crear</button>
      </form>
    </section>
  );
}
